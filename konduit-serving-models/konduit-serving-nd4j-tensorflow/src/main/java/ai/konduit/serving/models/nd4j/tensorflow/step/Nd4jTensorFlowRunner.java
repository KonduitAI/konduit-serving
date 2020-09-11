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

package ai.konduit.serving.models.nd4j.tensorflow.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ValueNotFoundException;
import ai.konduit.serving.pipeline.impl.pipeline.graph.Input;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.tensorflow.conversion.graphrunner.GraphRunnerServiceProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@CanRun(Nd4jTensorFlowStep.class)
public class Nd4jTensorFlowRunner implements PipelineStepRunner {

    private final Nd4jTensorFlowStep step;
    private GraphRunnerServiceProvider sess;
    private Map<String, INDArray> inputData;

    public Nd4jTensorFlowRunner(@NonNull Nd4jTensorFlowStep step) {
        this.step = step;
    }

    @Override
    public void close() {
        if (sess != null){
            sess.run(inputData).clear();
        }



    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        Preconditions.checkState(step.inputNames() != null, "TensorFlowStep input array names are not set (null)");

        try {

            File origFile = URIResolver.getFile(step.modelUri());
            Preconditions.checkState(origFile.exists(), "Model file does not exist: " + step.modelUri());

            byte[] bytes = FileUtils.readFileToByteArray(origFile);
            Map<String, String> inputDataTypes = getDataTypes(data, step.inputNames());

            this.sess.init(step.inputNames(), step.outputNames(), bytes, step.constants(), inputDataTypes);
            log.info("Loaded TensorFlow frozen model");
        } catch (Throwable t) {
            log.info("An error occurred during graph initialization: " + t);

        }


        this.inputData = new HashMap<String, INDArray>();
        for (String s : step.inputNames()) {
            if (data.type(s) == ValueType.NDARRAY){
                inputData.put(s, data.getNDArray(s).getAs(INDArray.class));

            }
        }



            this.sess.run(inputData).;

//        how to retrieve output ?



    }

    protected Map<String, String> getDataTypes(Data data, List<String> inputNames) {

        Map<String, String> inputDataTypes = new HashMap<String, String>();
        for (String s : inputNames) {
            if (!data.has(s)) {
                throw new ValueNotFoundException("Error in TensorFlowStep: Input data does not have a value corresponding to TensorFlowStep.inputNames value \"" +
                        s + "\" - data keys = " + data.keys());
            }
            if (data.type(s) != ValueType.NDARRAY) {
                String listType = data.type(s) == ValueType.LIST ? data.listType(s).toString() : null;
                throw new ValueNotFoundException("Error in TensorFlowStep (" + name() + "): Input data value corresponding to TensorFlowStep.inputNames value \"" +
                        s + "\" is not an NDArray type - is " + (listType == null ? data.type(s) : "List<" + listType + ">"));
            }

            inputDataTypes.put(s, data.type(s).toString());


        }

        return inputDataTypes;

    }


}
