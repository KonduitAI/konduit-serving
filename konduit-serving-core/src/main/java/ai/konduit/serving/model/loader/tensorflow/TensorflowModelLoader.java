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

package ai.konduit.serving.model.loader.tensorflow;

import ai.konduit.serving.model.SavedModelConfig;
import ai.konduit.serving.model.TensorDataType;
import ai.konduit.serving.model.TensorFlowConfig;
import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import io.vertx.core.buffer.Buffer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.io.FileUtils;
import org.nd4j.base.Preconditions;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Load models from tensorflow.
 *
 * @author Adam Gibson
 */
public class TensorflowModelLoader implements ModelLoader<TensorflowGraphHolder> {

    @Getter
    private File protoFile;
    @Getter
    private File configFile;
    @Singular
    @Getter
    private List<String> inputNames, outputNames;
    @Singular
    @Getter
    private Map<String, TensorDataType> castingInputTypes, castingOutputTypes;
    @Getter
    private SavedModelConfig savedModelConfig;


    /**
     * Load the model with the given inputs
     *
     * @param inputNames         the input names ot use for the model
     * @param outputNames        the output names for the model
     * @param protoFile          the protobuf file for the model
     * @param configFile         the session configuration for running the model (can be null)
     * @param savedModelConfig   the saved model configuration for the model (this can be null and should only be used
     *                           when proto file is null(
     * @param castingInputTypes  the input types to automatically cast inputs to before performing inference
     * @param castingOutputTypes the output types to automatically cast outputs to before returning results
     */
    @Builder
    public TensorflowModelLoader(List<String> inputNames,
                                 List<String> outputNames,
                                 File protoFile,
                                 File configFile,
                                 SavedModelConfig savedModelConfig,
                                 Map<String, TensorDataType> castingInputTypes,
                                 Map<String, TensorDataType> castingOutputTypes) {
        if (inputNames != null && outputNames != null)
            Preconditions.checkState(!inputNames.equals(outputNames), "Input names and output names should not be the same");
        this.protoFile = protoFile;
        this.configFile = configFile;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        this.savedModelConfig = savedModelConfig;
        this.castingInputTypes = castingInputTypes;
        this.castingOutputTypes = castingOutputTypes;
    }

    /**
     * Create a tensorflow model loader from the given
     * configuration
     *
     * @param modelPipelineStepConfig the configuration to create
     *                                the model loader with
     * @return the created tensorflow model loader
     */
    public static TensorflowModelLoader createFromConfig(ModelStep modelPipelineStepConfig) {
        TensorFlowConfig config = (TensorFlowConfig) modelPipelineStepConfig.getModelConfig();
        String sessionConfigPath = config.getConfigProtoPath();
        SavedModelConfig savedModelConfig = config.getSavedModelConfig();
        List<String> inputNames = modelPipelineStepConfig.getInputNames();
        List<String> outputNames = modelPipelineStepConfig.getOutputNames();
        String modelConfigPath = config.getModelConfigType().getModelLoadingPath();
        Preconditions.checkNotNull(modelConfigPath, "No model configuration path specified!");
        Preconditions.checkNotNull(inputNames, "No input names specified!");
        Preconditions.checkNotNull(outputNames, "No output names specified!");

        try {
            TensorflowModelLoader tensorflowModelLoader = TensorflowModelLoader.builder()
                    .savedModelConfig(savedModelConfig)
                    .inputNames(inputNames)
                    .outputNames(outputNames)
                    .castingInputTypes(config.getTensorDataTypesConfig() == null ? null : config.getTensorDataTypesConfig().getInputDataTypes())
                    .castingOutputTypes(config.getTensorDataTypesConfig() == null ? null : config.getTensorDataTypesConfig().getOutputDataTypes())
                    .savedModelConfig(savedModelConfig)
                    .configFile(sessionConfigPath != null ? new File(sessionConfigPath) : null)
                    .protoFile(modelConfigPath != null ? new File(modelConfigPath) : null)
                    .build();

            return tensorflowModelLoader;

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Buffer saveModel(TensorflowGraphHolder model) {
        return Buffer.buffer(model.getGraphDef().toByteArray());
    }

    @Override
    public TensorflowGraphHolder loadModel() throws Exception {
        //note that saved model config supplants the normal model loading configuration
        if (savedModelConfig == null) {
            Preconditions.checkNotNull(protoFile, "No model configuration path specified!");
            Preconditions.checkNotNull(inputNames, "No input names specified!");
            Preconditions.checkNotNull(outputNames, "No output names specified!");
        }

        TensorflowGraphHolder tensorflowGraphHolder = TensorflowGraphHolder.builder()
                .configProto(configFile == null ? null : FileUtils.readFileToByteArray(configFile))
                .graphContent(protoFile == null ? null : FileUtils.readFileToByteArray(protoFile))
                .inputNames(inputNames)
                .outputNames(outputNames)
                .savedModelConfig(savedModelConfig)
                .castingInputTypes(castingInputTypes)
                .castingOutputTypes(castingOutputTypes)
                .build();

        return tensorflowGraphHolder;
    }

}
