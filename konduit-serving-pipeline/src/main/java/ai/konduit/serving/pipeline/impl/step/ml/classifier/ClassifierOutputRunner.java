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
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.AllArgsConstructor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

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


        NDArray classifierOutput = data.getNDArray(inputName);

        boolean batch = false;
        if (classifierOutput.shape().length == 2 && classifierOutput.shape()[0] > 1) {
            batch = true;
        }

        // If not specified, the predicted class index as a string is used - i.e., "0", "1"
        List<String> labels = step.Labels();

        if (labels == null) {
            labels = new ArrayList<String>();
        }
        if (labels.isEmpty()) {
            for (int i = 0; i < classifierOutput.shape()[1]; i++) {
                labels.add(Integer.toString(i));
            }
        }


        if (!batch) {
            double[] classifierOutputArr = squeze(classifierOutput);
            double[] maxValueWithIdx = getMaxValueAndIndex(classifierOutputArr);
            double prob = maxValueWithIdx[0];
            long index = (long) maxValueWithIdx[1];
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
                data.putListDouble("allProbabilities", DoubleStream.of(classifierOutputArr).boxed().collect(Collectors.toList()));
            }
        }

        if (batch) {

            int bS = (int) classifierOutput.shape()[1];
            double[][] y = classifierOutput.getAs(double[][].class);

            List<Double> probs = new ArrayList<Double>();
            List<Long> indeces = new ArrayList<Long>();
            List<String> labelsList = new ArrayList<String>();
            List<NDArray> allPropabilities = new ArrayList<NDArray>();

            for (int i = 0; i < bS; i++) {
                double[] sample = y[i];
                double[] maxValueWithIdx = getMaxValueAndIndex(sample);
                double prob = maxValueWithIdx[0];
                long index = (long) maxValueWithIdx[1];
                String label = labels.get((int) index);

                probs.add(prob);
                indeces.add(index);
                labelsList.add(label);
                allPropabilities.add(NDArray.create(sample));
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


    public static double[] squeze(NDArray arr) {

        // we have [numClasses] array, so do not modify nothing
        if (arr.shape().length == 1) {
            return arr.getAs(double[].class);
        }

        // i.e we have [1, numClasses] array
        if (arr.shape().length == 2 && arr.shape()[0] == 1) {
            return arr.getAs(double[][].class)[0];
        }

        return null;

    }

    double[] getMaxValueAndIndex(double[] arr) {
        double max = arr[0];
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return new double[]{max, maxIdx};
    }


}





