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

package ai.konduit.serving.pipeline;

import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.model.PythonConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
import lombok.Singular;
import org.datavec.api.transform.schema.Schema;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PythonPipelineStep extends PipelineStep {

    @Getter
    @Setter
    @Singular
    private Map<String, PythonConfig> pythonConfigs;

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
    public PythonPipelineStep(PythonConfig pythonConfig, String[] inputColumnNames, SchemaType[] inputTypes,
                              String[] outputColumnNames, SchemaType[] outputTypes) throws Exception {
        String defaultName = "default";
        this.input(defaultName, inputColumnNames, inputTypes);
        this.output(defaultName, outputColumnNames, outputTypes);
        this.pythonConfig(defaultName, pythonConfig);
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
    public PythonPipelineStep(PythonConfig pythonConfig, String[] inputColumnNames, SchemaType[] inputTypes)
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
    public PythonPipelineStep(Schema inputSchema, Schema outputSchema,
                              PythonConfig pythonConfig) throws Exception {
        String defaultName = "default";
        this.input(defaultName, inputSchema);
        this.output(defaultName, outputSchema);
        this.pythonConfig(defaultName, pythonConfig);
    }


    /**
     * Define a single, named step for a Python pipeline.
     *
     * @param stepName input and output name for this step
     * @param pythonConfig Konduit PythonConfig
     * @param inputSchema DataVec Schema for data input
     * @param outputSchema DataVec Schema for data output
     * @return this python step
     * @throws Exception key error
     */
    public  PythonPipelineStep step(String stepName, PythonConfig pythonConfig,  Schema inputSchema,
                                    Schema outputSchema) throws Exception {
        this.input(stepName, inputSchema);
        this.output(stepName, outputSchema);
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
    public PythonPipelineStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                                   SchemaType[] inputTypes, String[] outputColumnNames, SchemaType[] outputTypes)
            throws Exception {
        this.input(stepName, inputColumnNames, inputTypes);
        this.output(stepName, outputColumnNames, outputTypes);
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

     * @throws Exception key error
     */
    public PythonPipelineStep step(String stepName, PythonConfig pythonConfig, String[] inputColumnNames,
                                   SchemaType[] inputTypes)
            throws Exception {
        this.input(stepName, inputColumnNames, inputTypes);
        this.output(stepName, new String[]{}, new SchemaType[]{});
        this.pythonConfig(stepName, pythonConfig);

        return this;
    }


    /**
     * Define a Python config for this step.
     *
     * @param inputName input name
     * @param pythonConfig Konduit PythonConfig
     * @return this Python step
     */
    public PythonPipelineStep pythonConfig(String inputName, PythonConfig pythonConfig) {
        if (pythonConfigs == null) {
            pythonConfigs = new HashMap<>();
        }
        pythonConfigs.put(inputName, pythonConfig);
        return this;
    }



    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.PythonPipelineStepRunner";
    }
}
