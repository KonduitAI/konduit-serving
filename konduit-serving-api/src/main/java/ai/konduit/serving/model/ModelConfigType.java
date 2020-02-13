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

import ai.konduit.serving.config.TextConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * The {@link ModelConfigType} has meta data for a ModelLoader
 * This includes the model loading path, the output adapter
 * type, and the model type. See {@link ModelConfig}
 * for more information.
 *
 * @author Adam Gibson
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelConfigType implements Serializable, TextConfig {

    @JsonProperty
    private ModelConfig.ModelType modelType;
    @JsonProperty
    private String modelLoadingPath;


    public static ModelConfigType computationGraph(String path) {
        return new ModelConfigType(ModelConfig.ModelType.COMPUTATION_GRAPH, path);
    }

    public static ModelConfigType multiLayerNetwork(String path) {
        return new ModelConfigType(ModelConfig.ModelType.MULTI_LAYER_NETWORK, path);
    }

    public static ModelConfigType keras(String path) {
        return new ModelConfigType(ModelConfig.ModelType.KERAS, path);
    }

    public static ModelConfigType tensorFlow(String path) {
        return new ModelConfigType(ModelConfig.ModelType.TENSORFLOW, path);
    }

    public static ModelConfigType sameDiff(String path) {
        return new ModelConfigType(ModelConfig.ModelType.SAMEDIFF, path);
    }

    public static ModelConfigType pmml(String path) {
        return new ModelConfigType(ModelConfig.ModelType.PMML, path);
    }

}

