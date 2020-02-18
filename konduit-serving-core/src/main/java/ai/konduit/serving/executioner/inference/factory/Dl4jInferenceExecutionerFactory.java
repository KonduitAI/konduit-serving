/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.MultiComputationGraphInferenceExecutioner;
import ai.konduit.serving.executioner.inference.MultiLayerNetworkInferenceExecutioner;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.loader.dl4j.cg.ComputationGraphModelLoader;
import ai.konduit.serving.model.loader.dl4j.mln.MultiLayerNetworkModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.DL4JModelValidator;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Dl4jInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
        File modelPath = new File(inferenceConfiguration.getModelConfigType().getModelLoadingPath());

        if(DL4JModelValidator.validateMultiLayerNetwork(modelPath).isValid()) {
            ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

            MultiLayerNetworkInferenceExecutioner inferenceExecutioner = new MultiLayerNetworkInferenceExecutioner();
            MultiLayerNetworkModelLoader multiLayerNetworkModelLoader = new MultiLayerNetworkModelLoader(modelPath);
            inferenceExecutioner.initialize(multiLayerNetworkModelLoader, parallelInferenceConfig);
            List<String> inputNames = Collections.singletonList("default");
            List<String> outputNames = Collections.singletonList("default");
            return new InitializedInferenceExecutionerConfig(inferenceExecutioner, inputNames, outputNames);
        } else if (DL4JModelValidator.validateComputationGraph(modelPath).isValid()){
            log.debug("Error loading multi layer network from file. Attempting to load computation graph instead.");
            ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

            ComputationGraphModelLoader computationGraphModelLoader = new ComputationGraphModelLoader(modelPath);
            MultiComputationGraphInferenceExecutioner inferenceExecutioner = new MultiComputationGraphInferenceExecutioner();
            inferenceExecutioner.initialize(computationGraphModelLoader, parallelInferenceConfig);

            ComputationGraph computationGraph2 = computationGraphModelLoader.loadModel();
            List<String> inputNames = computationGraph2.getConfiguration().getNetworkInputs();
            List<String> outputNames = computationGraph2.getConfiguration().getNetworkOutputs();
            log.info("Loaded computation graph with input names " + inputNames + " and output names " + outputNames);

            return InitializedInferenceExecutionerConfig.builder()
                    .inferenceExecutioner(inferenceExecutioner)
                    .inputNames(inputNames).outputNames(outputNames)
                    .build();
        } else {
            throw new IllegalStateException(String.format("The given file at path %s is not a valid DL4J model.",
                    modelPath.getAbsolutePath()));
        }
    }
}
