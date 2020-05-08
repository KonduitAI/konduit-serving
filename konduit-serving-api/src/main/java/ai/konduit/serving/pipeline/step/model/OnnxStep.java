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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Configuration step for models in ONNX format.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OnnxStep extends ModelStep {

    public static OnnxStep fromJson(String json){
        return ObjectMappers.fromJson(json, OnnxStep.class);
    }

    public static OnnxStep fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, OnnxStep.class);
    }

    @Override
    public String getInferenceExecutionerFactoryClassName() {
        return "ai.konduit.serving.executioner.inference.factory.OnnxInferenceExecutionerFactory";
    }
}
