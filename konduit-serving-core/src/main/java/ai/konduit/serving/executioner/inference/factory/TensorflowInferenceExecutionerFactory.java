/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.TensorflowInferenceExecutioner;
import ai.konduit.serving.model.TensorFlowConfig;
import ai.konduit.serving.model.loader.tensorflow.TensorflowGraphHolder;
import ai.konduit.serving.model.loader.tensorflow.TensorflowModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.threadpool.tensorflow.conversion.graphrunner.GraphRunner;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.base.Preconditions;

import java.util.List;

@Slf4j
public class TensorflowInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        TensorFlowConfig tensorFlowConfig = null;
        try {
            tensorFlowConfig = (TensorFlowConfig) modelPipelineStepConfig.getModelConfig();
        } catch (Exception e) {
            log.error("Could not extract TensorFlowConfig. Did you provide one to your verticle?");
        }

        log.debug("Loading model loader from configuration " + tensorFlowConfig);
        TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.createFromConfig(modelPipelineStepConfig);
        TensorflowInferenceExecutioner inferenceExecutioner = new TensorflowInferenceExecutioner();
        Preconditions.checkNotNull(modelPipelineStepConfig.getParallelInferenceConfig(), "No parallel inference config found on model pipeline step!");
        inferenceExecutioner.initialize(tensorflowModelLoader, modelPipelineStepConfig.getParallelInferenceConfig());

        /**
         * Automatically infer from model
         */
        TensorflowGraphHolder computationGraph = tensorflowModelLoader.loadModel();
        log.debug("Created model loader with inputs " + computationGraph.getInputNames() + " and output names " + computationGraph.getOutputNames());

        GraphRunner graphRunner = computationGraph.createRunner();
        List<String> inputNames = graphRunner.getInputOrder();
        List<String> outputNames = graphRunner.getOutputOrder();
        if (inputNames == null) {
            throw new IllegalStateException("No input names found from configuration!");
        }


        if (outputNames == null) {
            throw new IllegalStateException("No output names found from configuration!");
        }


        graphRunner.close();
        return new InitializedInferenceExecutionerConfig(inferenceExecutioner, inputNames, outputNames);
    }
}
