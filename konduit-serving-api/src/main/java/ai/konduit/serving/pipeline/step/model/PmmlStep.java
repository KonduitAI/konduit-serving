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

package ai.konduit.serving.pipeline.step.model;

import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.util.ObjectMappers;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PmmlStep extends ModelStep {

    public static final String DEFAULT_EVALUATOR_FACTORY = "org.jpmml.evaluator.ModelEvaluatorFactory";

    @Builder.Default
    private String evaluatorFactoryName = DEFAULT_EVALUATOR_FACTORY;

    public PmmlStep() {}

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.PmmlInferenceExecutionerStepRunner";
    }

    public static PmmlStep fromJson(String json){
        return ObjectMappers.fromJson(json, PmmlStep.class);
    }

    public static PmmlStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, PmmlStep.class);
    }

    @Override
    public String getInferenceExecutionerFactoryClassName() {
        return "ai.konduit.serving.executioner.inference.factory.PmmlInferenceExecutionerFactory";
    }
}
