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

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.util.python.PythonVariables;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.schema.Schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;



@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PythonStep extends PipelineStep {

    @Getter
    @Setter
    @Singular
    private Map<String, PythonConfig> pythonConfigs;

    public PythonStep(PythonConfig pythonConfig) throws Exception {
        this.step(pythonConfig);
    }

    @Override
    public PythonStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return (PythonStep) super.setInput("default", columnNames, types);
    }

    @Override
    public PythonStep setOutput(String[] columnNames, SchemaType[] types) throws Exception {
        return (PythonStep) super.setOutput("default", columnNames, types);
    }

    @Override
    public PythonStep setInput(Schema inputSchema) throws Exception {
        return (PythonStep) super.setInput("default", inputSchema);
    }

    @Override
    public PythonStep setOutput(Schema outputSchema) throws Exception {
        return (PythonStep) super.setOutput("default", outputSchema);
    }

    @Override
    public PythonStep setInput(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (PythonStep) super.setInput(inputName, columnNames, types);
    }

    @Override
    public PythonStep setOutput(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (PythonStep) super.setOutput(outputName, columnNames, types);
    }

    @Override
    public PythonStep setInput(String inputName, Schema inputSchema) throws Exception {
        return (PythonStep) super.setInput(inputName, inputSchema);
    }

    @Override
    public PythonStep setOutput(String outputName, Schema outputSchema) throws Exception {
        return (PythonStep) super.setOutput(outputName, outputSchema);
    }

    /**
     * Create a PythonConfig Step with default input and output names
     * from column names, schema types and the actual PythonConfig
     *
     * @param pythonConfig Konduit PythonConfig
     * @param inputColumnNames input column names
     * @param inputTypes input schema types
     * @param outputColumnNames output column names
     * @param outputTypes output schema types
     * @throws Exception key error
     */
    public PythonStep(PythonConfig pythonConfig, String[] inputColumnNames, SchemaType[] inputTypes,
                      String[] outputColumnNames, SchemaType[] outputTypes) throws Exception {
        String defaultName = "default";
        this.setInput(defaultName, inputColumnNames, inputTypes);
        this.setOutput(defaultName, outputColumnNames, outputTypes);
        this.pythonConfig(defaultName, pythonConfig);
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param pythonConfig Konduit PythonConfig
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
     * @param pythonConfig Konduit PythonConfig
     * @param inputSchema DataVec Schema for data input
     * @param outputSchema DataVec Schema for data output
     * @return this python step
     * @throws Exception key error
     */
    public PythonStep step(PythonConfig pythonConfig, Schema inputSchema,
                           Schema outputSchema) throws Exception {
        return this.step("default", pythonConfig, inputSchema, outputSchema);
    }

    /**
     * Create a PythonConfig Step with default input and output names
     * from input column names, input schema types and the actual PythonConfig
     *
     * @param pythonConfig Konduit PythonConfig
     * @param inputColumnNames input column names
     * @param inputTypes input schema types
     * @throws Exception key error
     */
    public PythonStep(PythonConfig pythonConfig, String[] inputColumnNames, SchemaType[] inputTypes)
            throws Exception {
        this(pythonConfig, inputColumnNames, inputTypes, new String[]{}, new SchemaType[]{});
    }


    /**
     * Create a PythonConfig Step with default input and output names
     * just from input/output schema and the actual PythonConfig
     *
     * @param inputSchema DataVec Schema for data input
     * @param outputSchema DataVec Schema for data output
     * @param pythonConfig Konduit PythonConfig
     * @throws Exception key error
     */
    public PythonStep(Schema inputSchema, Schema outputSchema,
                      PythonConfig pythonConfig) throws Exception {
        String defaultName = "default";
        this.setInput(defaultName, inputSchema);
        this.setOutput(defaultName, outputSchema);
        this.pythonConfig(defaultName, pythonConfig);
    }


    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName input and output name for this step
     * @param pythonConfig Konduit PythonConfig
     * @param inputSchema {@link Schema} for data input
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
     * @param stepName input and output name for this step
     * @param pythonConfig Konduit PythonConfig
     * @param inputColumnNames input column names
     * @param inputTypes inpput schema types
     * @param outputColumnNames output column names
     * @param outputTypes output schema types
     * @throws Exception key error
     */
    public PythonStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                           SchemaType[] inputTypes, String[] outputColumnNames, SchemaType[] outputTypes)
            throws Exception {
        this.setInput(stepName, inputColumnNames, inputTypes);
        this.setOutput(stepName, outputColumnNames, outputTypes);
        this.pythonConfig(stepName, pythonConfig);

        return this;
    }

    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName input and output name for this step
     * @param pythonConfig Konduit {@link PythonConfig}
     * @param inputColumnNames input column names
     * @param inputTypes input schema types

     * @throws Exception key error
     */
    public PythonStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                           SchemaType[] inputTypes)
            throws Exception {
        this.setInput(stepName, inputColumnNames, inputTypes);
        this.setOutput(stepName, new String[]{}, new SchemaType[]{});
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
     * @param inputName input name
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
     * @param stepName input and output name for this step
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


    private static SchemaType[] pythonToDataVecVarTypes(String [] pythonVarTypes) {
        return Arrays.stream(pythonVarTypes)
                .map(type -> pythonToDataVecVarTypes(PythonVariables.Type.valueOf(type)))
                .toArray(SchemaType[]::new);
    }

    private static ai.konduit.serving.config.SchemaType pythonToDataVecVarTypes(PythonVariables.Type pythonVarType) {
        try {
            switch (pythonVarType) {
                case BOOL:
                    return ai.konduit.serving.config.SchemaType.Boolean;
                case STR:
                    return ai.konduit.serving.config.SchemaType.String;
                case INT:
                    return ai.konduit.serving.config.SchemaType.Integer;
                case FLOAT:
                    return ai.konduit.serving.config.SchemaType.Float;
                case NDARRAY:
                    return ai.konduit.serving.config.SchemaType.NDArray;
                case LIST:
                case FILE:
                case DICT:
                default:
                    throw new IllegalArgumentException(String.format("Can't convert (%s) to (%s) enum",
                            pythonVarType.name(), ai.konduit.serving.config.SchemaType.class.getName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }




    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.PythonPipelineStepRunner";
    }
}
