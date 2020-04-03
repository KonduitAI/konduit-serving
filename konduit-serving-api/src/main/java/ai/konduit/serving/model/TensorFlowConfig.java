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

package ai.konduit.serving.model;

import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.nd4j.tensorflow.conversion.graphrunner.SavedModelConfig;

/**
 * TensorFlow extension of {@link ModelConfig}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TensorFlowConfig extends ModelConfig {

    public final static String TENSORFLOW_EXECUTION_CONFIG_KEY = "tensorFlowConfig";
    private String configProtoPath;
    private SavedModelConfig savedModelConfig;

    public static TensorFlowConfig fromJson(String json){
        return ObjectMappers.fromJson(json, TensorFlowConfig.class);
    }

    public static TensorFlowConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, TensorFlowConfig.class);
    }
}
