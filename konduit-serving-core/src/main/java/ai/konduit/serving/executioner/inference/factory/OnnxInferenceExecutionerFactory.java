/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
import ai.konduit.serving.executioner.inference.OnnxInferenceExecutioner;
import ai.konduit.serving.model.loader.OnnxModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.OnnxStep;

public class OnnxInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        OnnxStep onnxStep = (OnnxStep) modelPipelineStepConfig;
        ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

        String onnxConfigPath = onnxStep.getPath();

        OnnxInferenceExecutioner inferenceExecutioner = new OnnxInferenceExecutioner();
        OnnxModelLoader modelLoader1 = new OnnxModelLoader(onnxConfigPath);
        inferenceExecutioner.initialize(modelLoader1, parallelInferenceConfig);
        return new InitializedInferenceExecutionerConfig(inferenceExecutioner, null, null);
    }
}
