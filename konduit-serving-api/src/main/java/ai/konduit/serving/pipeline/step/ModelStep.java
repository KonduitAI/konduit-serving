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
import ai.konduit.serving.executioner.inference.factory.InferenceExecutionerFactory;
import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.output.types.ClassifierOutput;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.config.NormalizationConfig;
import ai.konduit.serving.util.ObjectMappers;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonAlias;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * {@link ModelStep} is for use with the various
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
 * Implementations of {@link ModelStep} contains the various kinds of model configurations that can be loaded
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
@SuperBuilder
@JsonIgnoreProperties({"map", "empty"})
@EqualsAndHashCode(callSuper = true)
public abstract class ModelStep extends BasePipelineStep<ModelStep> {

    @Singular
    private Map<String, TensorDataType> inputDataTypes;

    @Singular
    private Map<String, TensorDataType> outputDataTypes;

    @JsonProperty
    @JsonAlias({"modelLoadingPath", "model_loading_path"})
    private String path;

    @Builder.Default
    private ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

    private NormalizationConfig normalizationConfig;

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

    public ModelStep() {}

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

    protected abstract String getInferenceExecutionerFactoryClassName();

    public InferenceExecutionerFactory createExecutionerFactory() throws Exception {
        return (InferenceExecutionerFactory) Class
                .forName(getInferenceExecutionerFactoryClassName())
                .getConstructor()
                .newInstance();
    }
}
