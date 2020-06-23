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

package ai.konduit.serving.pipeline.impl.step.ml.regression;

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

import static ai.konduit.serving.pipeline.impl.step.ml.Utils.FloatNDArrayToDouble;
import static ai.konduit.serving.pipeline.impl.step.ml.Utils.squeeze;

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
            throw new UnsupportedOperationException("Invalid input to RegressionOutputStep: only rank 1 or 2 inputs are available, got array with shape" + Arrays.toString(regressionOutput.shape()));

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
            double[] regressionOutputArr = squeeze(regressionOutput);
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


}





