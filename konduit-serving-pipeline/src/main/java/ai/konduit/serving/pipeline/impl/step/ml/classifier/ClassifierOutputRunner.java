/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.step.ml.classifier;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.registry.MicrometerRegistry;
import ai.konduit.serving.pipeline.settings.KonduitSettings;
import ai.konduit.serving.pipeline.util.DataUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ai.konduit.serving.pipeline.util.NDArrayUtils.*;

@RequiredArgsConstructor
@CanRun(ClassifierOutputStep.class)
public class ClassifierOutputRunner implements PipelineStepRunner {

    @NonNull
    protected final ClassifierOutputStep step;

    private boolean metricsSetup = false;
    private MeterRegistry registry = null;
    private List<Counter> classificationMetricsCounters = new ArrayList<>();

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        String inputName = step.inputName();


        if (inputName == null) {
            String errMultipleKeys = "NDArray field name was not provided and could not be inferred: multiple NDArray fields exist: %s and %s";
            String errNoKeys = "NDArray field name was not provided and could not be inferred: no image NDArray exist";
            inputName = DataUtils.inferField(data, ValueType.NDARRAY, false, errMultipleKeys, errNoKeys);
        }

        String probName = step.probName() == null ? ClassifierOutputStep.DEFAULT_PROB_NAME : step.probName();
        String indexName = step.indexName() == null ? ClassifierOutputStep.DEFAULT_PROB_NAME : step.indexName();
        String labelName = step.labelName() == null ? ClassifierOutputStep.DEFAULT_PROB_NAME : step.labelName();


        NDArray classifierOutput = data.getNDArray(inputName);
        if (classifierOutput.shape().length > 2) {
            throw new UnsupportedOperationException("Invalid input to ClassifierOutputStep: only rank 1 or 2 inputs are available, got array with shape" + Arrays.toString(classifierOutput.shape()));

        }

        classifierOutput = FloatNDArrayToDouble(classifierOutput);


        boolean batch = false;
        if (classifierOutput.shape().length == 2 && classifierOutput.shape()[0] > 1) {
            batch = true;
        }

        // If not specified, the predicted class index as a string is used - i.e., "0", "1"
        List<String> labels = step.Labels();

        if (labels == null) {
            labels = new ArrayList<>();
        }
        if (labels.isEmpty()) {
            for (int i = 0; i < classifierOutput.shape()[1]; i++) {
                labels.add(Integer.toString(i));
            }
        }


        if(!metricsSetup) {
            registry = MicrometerRegistry.getRegistry();

            if(registry != null) {
                for (String label : labels) {
                    classificationMetricsCounters.add(Counter.builder(label)
                            .description("Classification counts seen so far for class label: " + label)
                            .tag("servingId", KonduitSettings.getServingId())
                            .baseUnit("classification.outcome")
                            .register(registry));
                }
            }

            metricsSetup = true;
        }

        if (!batch) {
            double[] classifierOutputArr = squeeze(classifierOutput);
            double[] maxValueWithIdx = getMaxValueAndIndex(classifierOutputArr);
            double prob = maxValueWithIdx[0];
            long index = (long) maxValueWithIdx[1];
            String label = labels.get((int) index);

            if(registry != null && index < classificationMetricsCounters.size()) {
                classificationMetricsCounters.get((int) index).increment();
            }

            if (step.topN() != null && step.topN() > 1) {
                if (step.returnProb()) {
                    data.putListDouble(probName, Collections.singletonList(prob));
                }
                if (step.returnIndex()) {
                    data.putListInt64(indexName, Collections.singletonList(index));
                }
                if (step.returnLabel()) {
                    data.putListString(labelName, Collections.singletonList(label));
                }
            } else {

                if (step.returnProb()) {
                    data.put(probName, prob);
                }
                if (step.returnIndex()) {
                    data.put(indexName, index);
                }
                if (step.returnLabel()) {
                    data.put(labelName, label);
                }

            }
            if (step.allProbabilities()) {
                data.put("allProbabilities", NDArray.create(classifierOutputArr));
            }
        }

        if (batch) {

            int bS = (int) classifierOutput.shape()[1];
            double[][] y = classifierOutput.getAs(double[][].class);

            List<Double> probs = new ArrayList<>();
            List<Long> indices = new ArrayList<>();
            List<String> labelsList = new ArrayList<>();
            List<NDArray> allProbabilities = new ArrayList<>();

            for (int i = 0; i < bS; i++) {
                double[] sample = y[i];
                double[] maxValueWithIdx = getMaxValueAndIndex(sample);
                double prob = maxValueWithIdx[0];
                long index = (long) maxValueWithIdx[1];
                String label = labels.get((int) index);

                if(registry != null && index < classificationMetricsCounters.size()) {
                    classificationMetricsCounters.get((int) index).increment();
                }

                probs.add(prob);
                indices.add(index);
                labelsList.add(label);
                allProbabilities.add(NDArray.create(sample));
            }


            if (step.returnProb()) {
                data.putListDouble(probName, probs);
            }
            if (step.returnIndex()) {
                data.putListInt64(indexName, indices);
            }
            if (step.returnLabel()) {
                data.putListString(labelName, labelsList);
            }
            if (step.allProbabilities()) {
                data.putListNDArray("allProbabilities", allProbabilities);
            }

        }


        return data;
    }







}





