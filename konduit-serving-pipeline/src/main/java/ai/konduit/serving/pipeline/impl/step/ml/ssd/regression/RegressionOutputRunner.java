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

package ai.konduit.serving.pipeline.impl.step.ml.ssd.regression;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@AllArgsConstructor
@CanRun(RegressionOutputStep.class)
public class RegressionOutputRunner implements PipelineStepRunner {

    protected final RegressionOutputStep step;


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


        NDArray regressionOutput = data.getNDArray(inputName);

        boolean batch = false;
        if (regressionOutput.shape().length == 2 && regressionOutput.shape()[0] > 1) {
            batch = true;
        }

        Map<String, Integer> outputNames = step.outputNames();

        if (outputNames == null) {
            outputNames = new HashMap<String, Integer>();
        }



        if (!batch) {
            double[] regressionOutputArr = squeze(regressionOutput);
            for (Map.Entry<String,Integer> entry : outputNames.entrySet())
                data.put(entry.getKey(), regressionOutputArr[entry.getValue()]);

        }

        if (batch) {

            int bS = (int) regressionOutput.shape()[1];
            double[][] y = regressionOutput.getAs(double[][].class);

            for (int i = 0; i < bS; i++) {

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





