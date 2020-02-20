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

package ai.konduit.serving.pipeline.step;

import ai.konduit.serving.config.Input.DataFormat;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.config.NormalizationConfig;
import ai.konduit.serving.util.ObjectMappers;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelStep extends BasePipelineStep<ModelStep> {

    private ModelConfig modelConfig;

    @Builder.Default
    private ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

    private NormalizationConfig normalizationConfig;

    public ModelStep() {
    }

    public ModelStep(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public PredictionType[] validPredictionTypes() {
        return PredictionType.values();
    }

    @Override
    public DataFormat[] validInputTypes() {
        return new DataFormat[] {
                DataFormat.NUMPY,
                DataFormat.ND4J,
                DataFormat.JSON,
                DataFormat.IMAGE,
                DataFormat.IMAGE
        };

    }

    @Override
    public Output.DataFormat[] validOutputTypes() {
        return new Output.DataFormat[] {
                Output.DataFormat.NUMPY,
                Output.DataFormat.ND4J,
                Output.DataFormat.JSON,
                Output.DataFormat.ARROW,
        };
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.InferenceExecutionerStepRunner";
    }

    public static ModelStep fromJson(String json){
        return ObjectMappers.fromJson(json, ModelStep.class);
    }

    public static ModelStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, ModelStep.class);
    }
}
