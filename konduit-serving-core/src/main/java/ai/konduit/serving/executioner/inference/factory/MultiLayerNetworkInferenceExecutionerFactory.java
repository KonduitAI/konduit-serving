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
import ai.konduit.serving.executioner.inference.MultiLayerNetworkInferenceExecutioner;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.loader.dl4j.mln.MultiLayerNetworkModelLoader;
import ai.konduit.serving.pipeline.ModelPipelineStep;
import ai.konduit.serving.config.ServingConfig;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MultiLayerNetworkInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelPipelineStep modelPipelineStepConfig) throws Exception {
        ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
        ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

        MultiLayerNetworkInferenceExecutioner inferenceExecutioner = new MultiLayerNetworkInferenceExecutioner();
        MultiLayerNetworkModelLoader multiLayerNetworkModelLoader = new MultiLayerNetworkModelLoader(new File(inferenceConfiguration.getModelConfigType().getModelLoadingPath()));
        inferenceExecutioner.initialize(multiLayerNetworkModelLoader, parallelInferenceConfig);
        List<String> inputNames = Collections.singletonList("default");
        List<String> outputNames = Collections.singletonList("default");
        return new InitializedInferenceExecutionerConfig(inferenceExecutioner,inputNames,outputNames);
    }
}
