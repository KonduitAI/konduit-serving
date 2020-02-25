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
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.util.ObjectMappers;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.transform.schema.Schema;
import org.datavec.python.PythonType;

import java.util.*;
import java.util.stream.Collectors;


/**
 * PythonStep defines a custom Python {@link PipelineStep}
 * from a {@link PythonConfig}.
 *
 * @author Adam Gibson
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class PythonStep extends BasePipelineStep<PythonStep> {

    @Getter
    @Setter
    @Singular
    private Map<String, PythonConfig> pythonConfigs;

    public PythonStep(PythonConfig pythonConfig) throws Exception {
        this.step(pythonConfig);
    }

    /**
     * Create a {@link PythonConfig} Step with default input and output names
     * from column names, schema types and the actual {@link PythonConfig}
     *
     * @param pythonConfig      {@link PythonConfig}
     * @param inputColumnNames  input column names
     * @param inputTypes        input schema types
     * @param outputColumnNames output column names
     * @param outputTypes       output schema types
     * @throws Exception key error
     */
    public PythonStep(PythonConfig pythonConfig, String[] inputColumnNames, List<SchemaType> inputTypes,
                      String[] outputColumnNames, List<SchemaType> outputTypes) throws Exception {
        String defaultName = "default";
        this.setInput(defaultName, inputColumnNames, inputTypes);
        this.setOutput(defaultName, outputColumnNames, outputTypes);
        this.pythonConfig(defaultName, pythonConfig);
    }

    /**
     * Create a PythonConfig Step with default input and output names
     * from input column names, input schema types and the actual PythonConfig
     *
     * @param pythonConfig     {@link PythonConfig}
     * @param inputColumnNames input column names
     * @param inputTypes       input schema types
     * @throws Exception key error
     */
    public PythonStep(PythonConfig pythonConfig, String[] inputColumnNames, List<SchemaType> inputTypes)
            throws Exception {
        this(pythonConfig, inputColumnNames, inputTypes, new String[]{}, Collections.emptyList());
    }

    /**
     * Create a PythonConfig Step with default input and output names
     * just from input/output schema and the actual PythonConfig
     *
     * @param inputSchema  {@link Schema} for data input
     * @param outputSchema {@link Schema} for data output
     * @param pythonConfig {@link PythonConfig}
     * @throws Exception key error
     */
    public PythonStep(Schema inputSchema, Schema outputSchema,
                      PythonConfig pythonConfig) throws Exception {
        String defaultName = "default";
        this.setInput(defaultName, inputSchema);
        this.setOutput(defaultName, outputSchema);
        this.pythonConfig(defaultName, pythonConfig);
    }

    private static List<SchemaType> pythonToDataVecVarTypes(String[] pythonVarTypes) {
        return Arrays.stream(pythonVarTypes)
                .map(PythonStep::pythonToDataVecVarTypes)
                .collect(Collectors.toList());
    }

    private static SchemaType pythonToDataVecVarTypes(String pythonTypeString) {
        try {
            switch (PythonType.valueOf(pythonTypeString).getName()) {
                case BOOL:
                    return SchemaType.Boolean;
                case STR:
                    return SchemaType.String;
                case INT:
                    return SchemaType.Integer;
                case FLOAT:
                    return SchemaType.Float;
                case NDARRAY:
                case LIST:
                    return SchemaType.NDArray;
                case DICT:
                default:
                    throw new IllegalArgumentException(String.format("Can't convert (%s) to (%s) enum",
                            pythonTypeString, ai.konduit.serving.config.SchemaType.class.getName()));
            }
        } catch (Exception e) {
            log.error("Unable to convert python type: " + pythonTypeString +
                    " into a valid datavec schema type. Error was: ", e);
        }

        return null;
    }

    @Override
    public PredictionType[] validPredictionTypes() {
        return new PredictionType[]{
                PredictionType.RAW,
                PredictionType.CLASSIFICATION,
                PredictionType.REGRESSION
        };
    }

    @Override
    public DataFormat[] validInputTypes() {
        return new DataFormat[]{
                DataFormat.ARROW,
                DataFormat.NUMPY,
                DataFormat.JSON,
                DataFormat.IMAGE
        };
    }

    @Override
    public Output.DataFormat[] validOutputTypes() {
        return new Output.DataFormat[]{
                Output.DataFormat.ARROW,
                Output.DataFormat.ND4J,
                Output.DataFormat.NUMPY,
                Output.DataFormat.JSON
        };
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param pythonConfig {@link PythonConfig}
     * @throws Exception key error
     */
    public PythonStep step(PythonConfig pythonConfig)
            throws Exception {
        this.step("default", pythonConfig);

        return this;
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param pythonConfig {@link PythonConfig}
     * @param inputSchema  {@link Schema} for data input
     * @param outputSchema {@link Schema} for data output
     * @return this python step
     * @throws Exception key error
     */
    public PythonStep step(PythonConfig pythonConfig, Schema inputSchema,
                           Schema outputSchema) throws Exception {
        return this.step("default", pythonConfig, inputSchema, outputSchema);
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName     input and output name for this step
     * @param pythonConfig {@link PythonConfig}
     * @param inputSchema  {@link Schema} for data input
     * @param outputSchema {@link Schema} for data output
     * @return this python step
     * @throws Exception key error
     */
    public PythonStep step(String stepName,
                           PythonConfig pythonConfig,
                           Schema inputSchema,
                           Schema outputSchema) throws Exception {
        this.setInput(stepName, inputSchema);
        this.setOutput(stepName, outputSchema);
        this.pythonConfig(stepName, pythonConfig);

        return this;
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName          input and output name for this step
     * @param pythonConfig      {@link PythonConfig}
     * @param inputColumnNames  input column names
     * @param inputTypes        input schema types
     * @param outputColumnNames output column names
     * @param outputTypes       output schema types
     * @throws Exception key error
     */
    public PythonStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                           List<SchemaType> inputTypes, String[] outputColumnNames, List<SchemaType> outputTypes)
            throws Exception {
        this.setInput(stepName, inputColumnNames, inputTypes);
        this.setOutput(stepName, outputColumnNames, outputTypes);
        this.pythonConfig(stepName, pythonConfig);

        return this;
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName         input and output name for this step
     * @param pythonConfig     Konduit {@link PythonConfig}
     * @param inputColumnNames input column names
     * @param inputTypes       input schema types
     * @throws Exception key error
     */
    public PythonStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                           List<SchemaType> inputTypes)
            throws Exception {
        this.setInput(stepName, inputColumnNames, inputTypes);
        this.setOutput(stepName, new String[]{}, Collections.emptyList());
        this.pythonConfig(stepName, pythonConfig);

        return this;
    }

    /**
     * Define a Python config for this step.
     *
     * @param pythonConfig Konduit {@link PythonConfig}
     * @return this Python step
     */
    public PythonStep pythonConfig(PythonConfig pythonConfig) {
        if (pythonConfigs == null) {
            pythonConfigs = new HashMap<>();
        }
        pythonConfigs.put("default", pythonConfig);
        return this;
    }

    /**
     * Define a Python config for this step.
     *
     * @param inputName    input name
     * @param pythonConfig Konduit PythonConfig
     * @return this Python step
     */
    public PythonStep pythonConfig(String inputName, PythonConfig pythonConfig) {
        if (pythonConfigs == null) {
            pythonConfigs = new HashMap<>();
        }
        pythonConfigs.put(inputName, pythonConfig);
        return this;
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName     input and output name for this step
     * @param pythonConfig {@link PythonConfig}
     * @throws Exception key error
     */
    public PythonStep step(String stepName, PythonConfig pythonConfig)
            throws Exception {

        Map<String, String> pythonInputs = pythonConfig.getPythonInputs(),
                pythonOutputs = pythonConfig.getPythonOutputs();

        this.step(stepName, pythonConfig,
                pythonInputs.keySet().toArray(new String[0]),
                pythonToDataVecVarTypes(pythonInputs.values().toArray(new String[pythonInputs.size()])),
                pythonOutputs.keySet().toArray(new String[0]),
                pythonToDataVecVarTypes(pythonOutputs.values().toArray(new String[pythonOutputs.size()])));

        return this;
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.PythonStepRunner";
    }

    public static PythonStep fromJson(String json) {
        return ObjectMappers.fromJson(json, PythonStep.class);
    }

    public static PythonStep fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, PythonStep.class);
    }
}
