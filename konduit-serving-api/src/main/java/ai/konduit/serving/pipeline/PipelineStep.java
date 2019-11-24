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
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

/** PipelineStep collects all ETL related properties (input schema,
 * normalization and transform steps, output schema, potential pre-
 * or post-processing etc.). This config is passed to the respective
 * verticle along with Model and Serving configurations.
 *
 * @author Adam Gibson
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonSubTypes({
        @JsonSubTypes.Type(value= ImageLoading.class, name = "ImageLoading"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= NormalizationConfig.class, name = "NormalizationConfig"),
        @JsonSubTypes.Type(value= ObjectDetectionConfig.class, name = "ObjectDetectionConfig"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= PythonPipelineStep.class, name = "PythonPipelineStep"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= TransformProcessPipelineStep.class, name = "TransformProcessPipelineStep"),
        @JsonSubTypes.Type(value= ArrayConcatenationStep.class, name = "ArrayConcatenationStep"),
        @JsonSubTypes.Type(value= PmmlPipelineStep.class, name = "PmmlPipelineStep"),
        @JsonSubTypes.Type(value= CustomPipelineStep.class, name = "CustomPipelineStep"),


})
@JsonTypeInfo(use = NAME, include = PROPERTY)
@EqualsAndHashCode(callSuper = false)
public abstract class PipelineStep implements Serializable {

    @Singular
    private Map<String, SchemaType[]> inputSchemas, outputSchemas;

    @Singular
    private List<String> inputNames, outputNames;

    @Singular
    private Map<String,List<String>> inputColumnNames,outputColumnNames;


    private void setInputColumns(String inputName, List<String> columnNames) {
        Map<String, List<String>> inputCols = this.getInputColumnNames();
        if (inputCols == null) {
            inputCols = new HashMap<>();
        }
        inputCols.put(inputName, columnNames);
        this.setInputColumnNames(inputCols);
    }

    private void setOutputColumns(String outputName, List<String> columnNames) {
        Map<String, List<String>> outputCols = this.getOutputColumnNames();
        if (outputCols == null) {
            outputCols = new HashMap<>();
        }
        outputCols.put(outputName, columnNames);
        this.setOutputColumnNames(outputCols);
    }

    private void setInputTypes(String inputName, SchemaType[] types) {
        Map<String, SchemaType[]> schemas = this.getInputSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
        }
        schemas.put(inputName, types);
        this.setInputSchemas(schemas);
    }

    private void setOutputTypes(String outputName, SchemaType[] types) {
        Map<String, SchemaType[]> schemas = this.getOutputSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
        }
        schemas.put(outputName, types);
        this.setOutputSchemas(schemas);
    }

    /**
     * Define a single input for a PipelineStep from column names and types.
     * The input name will be "default" when using this method.
     *
     * @param columnNames column names
     * @param types schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return this.setInput("default", columnNames, types);
    }

    /**
     * Define a single input for a PipelineStep from a schema.
     * The input name will be "default" when using this method.
     *
     * @param inputSchema input schema
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setInput(Schema inputSchema) throws Exception {
        return this.setInput("default", inputSchema);
    }

    /**
     * Define a single input for a TransformProcess Step from explicit
     * column names and types for this input.
     *
     * @param inputName input name
     * @param columnNames column names
     * @param types schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setInput(String inputName, String[] columnNames, SchemaType[] types)
            throws Exception {

        List<String> names = getInputNames();
        if (names == null) {
            names = new ArrayList<>();
        }
        if (!names.contains(inputName)) {

            names.add(inputName);
            this.setInputNames(names);

            setInputColumns(inputName, Arrays.asList(columnNames));
            setInputTypes(inputName, types);

            return this;
        } else {
            throw new Exception("Input name " + inputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     * Define a single input for a TransformProcess Step from a schema.
     *
     * @param inputName input name
     * @param inputSchema input schema
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setInput(String inputName, Schema inputSchema) throws Exception {

        List<String> names = getInputNames();
        if (names == null) {
            names = new ArrayList<>();
        }
        if (!names.contains(inputName)) {

            names.add(inputName);
            this.setInputNames(names);

            List<String> columnNames = SchemaTypeUtils.columnNames(inputSchema);
            setInputColumns(inputName, columnNames);

            SchemaType[] types = SchemaTypeUtils.typesForSchema(inputSchema);
            setInputTypes(inputName, types);

            return this;
        } else {
            throw new Exception("Input name " + inputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     * Define a single output for a PipelineStep from explicit
     * column names and types for this output. The output name
     * for this step will be "default".
     *
     * @param columnNames column names
     * @param types schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setOutput(String[] columnNames, SchemaType[] types)
            throws Exception {
        return this.setOutput("default", columnNames, types);
    }

    /**
     * Define a single output for a PipelineStep from a schema.
     * The output name for this step will be "default".
     *
     * @param outputSchema output schema
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setOutput(Schema outputSchema) throws Exception {
        return this.setOutput("default", outputSchema);
    }

    /**
     * Define a single output for a TransformProcess Step from explicit
     * column names and types for this output.
     *
     * @param outputName output name
     * @param columnNames column names
     * @param types schema types
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setOutput(String outputName, String[] columnNames, SchemaType[] types)
            throws Exception {

        List<String> names = getOutputNames();
        if (names == null) {
            names = new ArrayList<>();
        }
        if (!names.contains(outputName)) {

            names.add(outputName);
            this.setOutputNames(names);

            setOutputColumns(outputName, Arrays.asList(columnNames));
            setOutputTypes(outputName, types);

            return this;
        } else {
            throw new Exception("Output name " + outputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }


    /**
     * Define a single output for a TransformProcess Step.
     *
     * @param outputName output name
     * @param outputSchema output schema
     * @return this pipeline step
     * @throws Exception key error
     */
    public PipelineStep setOutput(String outputName, Schema outputSchema) throws Exception {

        List<String> names = getOutputNames();
        if (names == null) {
            names = new ArrayList<>();
        }
        if (!names.contains(outputName)) {

            names.add(outputName);
            this.setOutputNames(names);

            List<String> columnNames = SchemaTypeUtils.columnNames(outputSchema);
            setOutputColumns(outputName, columnNames);

            SchemaType[] types = SchemaTypeUtils.typesForSchema(outputSchema);
            setOutputTypes(outputName, types);

            return this;
        } else {
            throw new Exception("Output name " + outputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    public Schema outputSchemaForName(String name) {
        Preconditions.checkNotNull(outputSchemas,"No output schemas specified in configuration!");

        if(!outputSchemas.containsKey(name))
            return null;
        return SchemaTypeUtils.toSchema(outputSchemas.get(name),
                outputColumnNames.get(name));
    }

    public Schema inputSchemaForName(String name) {
        Preconditions.checkNotNull(inputSchemas,"No input schemas specified in configuration!");
        if(!inputSchemas.containsKey(name))
            return null;

        return SchemaTypeUtils.toSchema(inputTypesForName(name),
                inputColumnNames.get(name));
    }

    public SchemaType[] inputTypesForName(String name) {
        if(!inputSchemas.containsKey(name)) {
            return null;
        }

        return inputSchemas.get(name);
    }

    public boolean hasInputName(String name) {
        return inputNames.contains(name);
    }

    public String inputNameAt(int i) {
        Preconditions.checkState(!inputNames.isEmpty(),"Input names must not be empty!");
        return inputNames.get(i);
    }


    public boolean processColumn(String name,int index) {
        if(inputColumnNames.isEmpty())
            return true;

        if(!inputColumnNames.containsKey(name)) {
            throw new IllegalStateException("Input column names does not contain " + name);
        }

        String columnNameAtIndex = inputColumnNames.get(name).get(index);
        return inputColumnNames.get(name).contains(columnNameAtIndex);
    }

    public boolean inputNameIsValidForStep(String name) {
        int idx = inputNames.indexOf(name);
        return idx >= 0;
    }

    public String inputNameAtIndex(int idx) {
        return inputNames.get(idx);
    }

    public abstract String pipelineStepClazz();

    /**
     * Get the respective runner for the configuration
     * @return the respective step runner
     */
    public PipelineStepRunner getRunner() {
        try {
            Class<? extends PipelineStepRunner> clazz = (Class<? extends PipelineStepRunner>) Class.forName(this.pipelineStepClazz());
            Constructor constructor = clazz.getConstructor(PipelineStep.class);
            return (PipelineStepRunner) constructor.newInstance(this);
        } catch(Exception e) {
            throw new IllegalStateException("Unable to instantiate pipeline step from class " + this.pipelineStepClazz(), e);
        }
    }
}
