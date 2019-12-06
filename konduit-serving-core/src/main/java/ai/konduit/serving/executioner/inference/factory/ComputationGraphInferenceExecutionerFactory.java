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

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.executioner.inference.MultiComputationGraphInferenceExecutioner;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.loader.dl4j.cg.ComputationGraphModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;

import java.io.File;
import java.util.List;

@Slf4j
public class ComputationGraphInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
        ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

        ComputationGraphModelLoader computationGraphModelLoader = new ComputationGraphModelLoader(new File(inferenceConfiguration.getModelConfigType().getModelLoadingPath()));
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
    }
}
