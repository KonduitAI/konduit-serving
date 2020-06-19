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
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.shade.protobuf.common.primitives.Doubles;
import oshi.util.LsofUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@CanRun(ClassifierOutputStep.class)
public class ClassifierOutputRunner implements PipelineStepRunner {

    protected final ClassifierOutputStep step;


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


        INDArray classifierOutput = data.getNDArray(inputName).getAs(INDArray.class);

        boolean batch = false;
        if (classifierOutput.shape().length == 2 && classifierOutput.shape()[0] > 1) {
            batch = true;
        }

        // If not specified, the predicted class index as a string is used - i.e., "0", "1"
        List<String> labels = step.Labels();
        if (labels.isEmpty()) {
            for (int i = 0; i < classifierOutput.shape()[1]; i++) {
                labels.add(Integer.toString(i));
            }
        }


        if (!batch) {
            Nd4j.squeeze(classifierOutput, 0);
            double prob = classifierOutput.maxNumber().doubleValue();
            long index = classifierOutput.argMax(0).getLong(0);
            String label = labels.get((int) index);

            if (step.topN() != null && step.topN() > 1) {
                if (step.returnProb()) {
                    data.putListDouble(step.probName(), Collections.singletonList(prob));
                }
                if (step.returnIndex()) {
                    data.putListInt64(step.indexName(), Collections.singletonList(index));
                }
                if (step.returnLabel()) {
                    data.putListString(step.labelName(), Collections.singletonList(label));
                }
            } else {

                if (step.returnProb()) {
                    data.put(step.probName(), prob);
                }
                if (step.returnIndex()) {
                    data.put(step.indexName(), index);
                }
                if (step.returnLabel()) {
                    data.put(step.labelName(), label);
                }

            }
            if (step.allProbabilities()) {
                data.putListDouble("allProbabilities", Doubles.asList(classifierOutput.data().dup().asDouble()));
            }
        }

            if (batch) {

                int bS = (int) classifierOutput.shape()[1];

                List<Double> probs = new ArrayList<Double>();
                List<Long> indeces = new ArrayList<Long>();
                List<String> labelsList = new ArrayList<String>();
                List<NDArray> allPropabilities = new ArrayList<NDArray>();

                for (int i = 0; i < bS; i++) {
                    INDArray y = classifierOutput.get(NDArrayIndex.point(i), NDArrayIndex.all());

                    double prob = y.maxNumber().doubleValue();
                    long index = y.argMax(0).getLong(0);
                    String label = labels.get((int) index);

                    probs.add(prob);
                    indeces.add(index);
                    labelsList.add(label);
                    allPropabilities.add(NDArray.create(y.data().dup().asDouble()));
                }


                if (step.returnProb()) {
                    data.putListDouble(step.probName(), probs);
                }
                if (step.returnIndex()) {
                    data.putListInt64(step.indexName(), indeces);
                }
                if (step.returnLabel()) {
                    data.putListString(step.labelName(), labelsList);
                }
                if (step.allProbabilities()) {
                    data.putListNDArray("allProbabilities", allPropabilities);
                }

            }


            return data;
        }
    }





