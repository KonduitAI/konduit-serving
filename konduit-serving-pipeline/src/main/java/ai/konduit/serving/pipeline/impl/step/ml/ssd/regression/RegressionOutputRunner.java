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
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
        if (regressionOutput.shape().length > 2) {
            throw new UnsupportedOperationException("Invalid input to ClassifierOutputStep: only rank 1 or 2 inputs are available, got array with shape" + Arrays.toString(regressionOutput.shape()));

        }
        regressionOutput = FloatNDArrayToDouble(regressionOutput);


        boolean batch = false;
        if (regressionOutput.shape().length == 2 && regressionOutput.shape()[0] > 1) {
            batch = true;
        }

        Map<String, Integer> outputNames = step.names();

        if (outputNames == null || outputNames.isEmpty()) {
            throw new UnsupportedOperationException("RegressionOutputStep names field was not provided or is null");
        }


        if (!batch) {
            double[] regressionOutputArr = squeze(regressionOutput);
            for (Map.Entry<String, Integer> entry : outputNames.entrySet())
                data.put(entry.getKey(), regressionOutputArr[entry.getValue()]);

        }

        if (batch) {

            int bS = (int) regressionOutput.shape()[0];
            double[][] y = regressionOutput.getAs(double[][].class);

            for (Map.Entry<String, Integer> entry : outputNames.entrySet()) {
                List<Double> list = new ArrayList<Double>();
                for (int i = 0; i < bS; i++) {
                    list.add(y[i][entry.getValue()]);
                }
                data.putListDouble(entry.getKey(), list);

            }


        }


        return data;
    }


    private static double[] squeze(NDArray arr) {

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

    private static NDArray FloatNDArrayToDouble(NDArray ndarr) {
        if (ndarr.type() == NDArrayType.FLOAT || ndarr.type() == NDArrayType.FLOAT16 || ndarr.type() == NDArrayType.BFLOAT16) {
            float[][] farr = ndarr.getAs(float[][].class);
            double[][] darr = new double[(int) ndarr.shape()[0]][(int) ndarr.shape()[1]];
            for (int i = 0; i < farr.length; i++) {
                for (int j = 0; j < farr[i].length; j++) {
                    darr[i][j] = round(Double.valueOf(farr[i][j]),7);
                }
            }

            return NDArray.create(darr);
        }
        return ndarr;
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


}





