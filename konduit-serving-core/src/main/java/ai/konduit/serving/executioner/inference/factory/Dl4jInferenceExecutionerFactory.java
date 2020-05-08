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
import ai.konduit.serving.model.loader.dl4j.cg.ComputationGraphModelLoader;
import ai.konduit.serving.model.loader.dl4j.mln.MultiLayerNetworkModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.Dl4jStep;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.DL4JModelValidator;
import org.nd4j.common.validation.ValidationResult;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Dl4jInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        Dl4jStep dl4jStep = (Dl4jStep) modelPipelineStepConfig;
        File modelPath = new File(dl4jStep.getPath());

        ValidationResult mlnValidationResult = DL4JModelValidator.validateMultiLayerNetwork(modelPath);
        ValidationResult cgValidationResult;

        if (mlnValidationResult.isValid()) {
            ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

            MultiLayerNetworkInferenceExecutioner inferenceExecutioner = new MultiLayerNetworkInferenceExecutioner();
            MultiLayerNetworkModelLoader multiLayerNetworkModelLoader = new MultiLayerNetworkModelLoader(modelPath);
            inferenceExecutioner.initialize(multiLayerNetworkModelLoader, parallelInferenceConfig);
            List<String> inputNames = Collections.singletonList("default");
            List<String> outputNames = Collections.singletonList("default");
            return new InitializedInferenceExecutionerConfig(inferenceExecutioner, inputNames, outputNames);
        } else {
            log.info("Error loading MultiLayerNetwork from file: {}. Attempting to load as ComputationGraph instead...",
                    modelPath.getAbsoluteFile());
            cgValidationResult = DL4JModelValidator.validateComputationGraph(modelPath);

            if (cgValidationResult.isValid()) {
                ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

                ComputationGraphModelLoader computationGraphModelLoader = new ComputationGraphModelLoader(modelPath);
                MultiComputationGraphInferenceExecutioner inferenceExecutioner = new MultiComputationGraphInferenceExecutioner();
                inferenceExecutioner.initialize(computationGraphModelLoader, parallelInferenceConfig);

                ComputationGraph computationGraph2 = computationGraphModelLoader.loadModel();
                List<String> inputNames = computationGraph2.getConfiguration().getNetworkInputs();
                List<String> outputNames = computationGraph2.getConfiguration().getNetworkOutputs();
                log.info("Loaded computation graph with input names {} and output names {}", inputNames, outputNames);

                return InitializedInferenceExecutionerConfig.builder()
                        .inferenceExecutioner(inferenceExecutioner)
                        .inputNames(inputNames).outputNames(outputNames)
                        .build();
            } else {
                String finalErrorMessage = String.format("The given file at path %s is not a valid DL4J model.\n" +
                                " --- Errors while loading file as a MultiLayerNetwork ---\n%s\n" +
                                " --- Errors while loading file as a ComputationGraph ---\n%s",
                        modelPath.getAbsolutePath(), mlnValidationResult.toString(), cgValidationResult.toString());

                throw new IllegalStateException(finalErrorMessage);
            }
        }
    }
}
