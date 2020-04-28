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

package ai.konduit.serving.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ai.konduit.serving.util.ObjectMappers;

/**
 * Configuration for models in ONNX format.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class OnnxConfig extends ModelConfig {

    public static OnnxConfig fromJson(String json){
        return ObjectMappers.fromJson(json, OnnxConfig.class);
    }

    public static OnnxConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, OnnxConfig.class);
    }

    @Override
    public String getInferenceExecutionerFactoryClassName() {
        return "ai.konduit.serving.executioner.inference.factory.OnnxInferenceExecutionerFactory";
    }
}
