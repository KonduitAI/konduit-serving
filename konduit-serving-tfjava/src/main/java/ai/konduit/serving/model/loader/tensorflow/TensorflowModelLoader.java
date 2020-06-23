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

import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.TensorFlowStep;
import io.vertx.core.buffer.Buffer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.tensorflow.conversion.TensorDataType;
import org.nd4j.tensorflow.conversion.graphrunner.SavedModelConfig;

import java.io.File;
import java.util.HashMap;
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
        TensorFlowStep tensorFlowStep = (TensorFlowStep) modelPipelineStepConfig;
        String sessionConfigPath = tensorFlowStep.configProtoPath();
        SavedModelConfig savedModelConfig = tensorFlowStep.savedModelConfig();
        List<String> inputNames = modelPipelineStepConfig.getInputNames();
        List<String> outputNames = modelPipelineStepConfig.getOutputNames();
        String modelConfigPath = tensorFlowStep.getPath();
        Preconditions.checkNotNull(modelConfigPath, "No model configuration path specified!");
        Preconditions.checkNotNull(inputNames, "No input names specified!");
        Preconditions.checkNotNull(outputNames, "No output names specified!");

        try {
            return TensorflowModelLoader.builder()
                    .savedModelConfig(savedModelConfig)
                    .inputNames(inputNames)
                    .outputNames(outputNames)
                    .castingInputTypes(convertDTypes(tensorFlowStep.getInputDataTypes()))
                    .castingOutputTypes(convertDTypes(tensorFlowStep.getOutputDataTypes()))
                    .savedModelConfig(savedModelConfig)
                    .configFile(sessionConfigPath != null ? new File(sessionConfigPath) : null)
                    .protoFile(modelConfigPath != null ? new File(modelConfigPath) : null)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String,org.nd4j.tensorflow.conversion.TensorDataType> convertDTypes(Map<String, ai.konduit.serving.model.TensorDataType> in){
        if(in == null)
            return null;
        Map<String,org.nd4j.tensorflow.conversion.TensorDataType> out = new HashMap<>();
        for(Map.Entry<String, ai.konduit.serving.model.TensorDataType> e : in.entrySet()){
            out.put(e.getKey(), e.getValue().toTFType());
        }
        return out;
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

        return TensorflowGraphHolder.builder()
                .configProto(configFile == null ? null : FileUtils.readFileToByteArray(configFile))
                .graphContent(protoFile == null ? null : FileUtils.readFileToByteArray(protoFile))
                .inputNames(inputNames)
                .outputNames(outputNames)
                .savedModelConfig(savedModelConfig)
                .castingInputTypes(castingInputTypes)
                .castingOutputTypes(castingOutputTypes)
                .build();
    }

}
