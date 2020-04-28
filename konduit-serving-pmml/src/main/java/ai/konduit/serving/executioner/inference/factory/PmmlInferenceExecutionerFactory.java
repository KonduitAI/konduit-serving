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
import ai.konduit.serving.executioner.inference.PmmlInferenceExecutioner;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.PmmlConfig;
import ai.konduit.serving.model.loader.pmml.PmmlModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import org.jpmml.evaluator.ModelEvaluatorFactory;

import java.io.File;

public class PmmlInferenceExecutionerFactory implements InferenceExecutionerFactory {

    @Override
    public InitializedInferenceExecutionerConfig create(ModelStep modelPipelineStepConfig) throws Exception {
        ModelConfig inferenceConfiguration = modelPipelineStepConfig.getModelConfig();
        ParallelInferenceConfig parallelInferenceConfig = modelPipelineStepConfig.getParallelInferenceConfig();

        String pmmlConfigPath = inferenceConfiguration.getPath();
        ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
        PmmlInferenceExecutioner inferenceExecutioner = new PmmlInferenceExecutioner();
        PmmlModelLoader modelLoader1 = new PmmlModelLoader(modelEvaluatorFactory, new File(pmmlConfigPath));
        inferenceExecutioner.initialize(modelLoader1, parallelInferenceConfig);
        return new InitializedInferenceExecutionerConfig(inferenceExecutioner, null, null);
    }
}
