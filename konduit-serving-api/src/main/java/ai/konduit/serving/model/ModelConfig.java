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

import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.output.types.ClassifierOutput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * ModelLoadingConfiguration is for use with the various
 * {@link io.vertx.core.Verticle} found in the model server.
 * A range of enums are defined that encompass the model server workflow including:
 * <p>
 * {@link Output.PredictionType} which covers converting raw results
 * from InferenceExecutioner
 * to output consumable as json for application consumers.
 * A simple example of this would be converting a raw {@link org.nd4j.linalg.api.ndarray.INDArray}
 * containing probabilities to a {@link ClassifierOutput}
 * which contains human readable labels directly consumable from the json.
 * <p>
 * {@link ModelType} contains the various kinds of models that can be loaded
 * by the model server
 * <p>
 * JsonInputType covers the 2 major supported input types,
 * which are {@link io.vertx.core.json.JsonArray} of {@link io.vertx.core.json.JsonObject}
 * as named key value pairs, or {@link io.vertx.core.json.JsonArray} of
 * {@link io.vertx.core.json.JsonArray} which are index based values.
 * <p>
 * We also support variants of each of these input types with _ERROR
 * which will be slower for inference, but will also ignore invalid rows
 * that are found. These _ERROR endpoints will output skipped rows
 * where errors were encountered so users can fix problems with input data konduit-serving.
 *
 * @author Adam Gibson
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"map", "empty"})
@JsonSubTypes({
        @JsonSubTypes.Type(value = PmmlConfig.class, name = "PmmlConfig"),
        @JsonSubTypes.Type(value = SameDiffConfig.class, name = "SameDiffConfig"),
        @JsonSubTypes.Type(value = TensorFlowConfig.class, name = "TensorFlowConfig"),
        @JsonSubTypes.Type(value = KerasConfig.class, name = "KerasConfig"),
        @JsonSubTypes.Type(value = DL4JConfig.class, name= "DL4JConfig")
})
@JsonTypeInfo(use = NAME, include = PROPERTY)
@SuperBuilder
public abstract class ModelConfig implements Serializable, TextConfig {

    @JsonProperty
    private TensorDataTypesConfig tensorDataTypesConfig;

    @JsonProperty
    private ModelConfigType modelConfigType;

    public enum ModelType {
        DL4J,
        PMML,
        TENSORFLOW,
        KERAS,
        SAMEDIFF
    }


}
