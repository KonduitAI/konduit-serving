/*
 *
 *  * ******************************************************************************
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

import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.base.Preconditions;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * PipelineStep collects all ETL related properties (input schema,
 * normalization and transform steps, output schema, potential pre-
 * or post-processing etc.). This config is passed to the respective
 * verticle along with Model and Serving configurations.
 *
 * @author Adam Gibson
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@Slf4j
public abstract class BasePipelineStep<T extends BasePipelineStep<T>> implements PipelineStep<T> {

    @Singular
    protected Map<String, List<SchemaType>> inputSchemas;
    @Singular
    protected Map<String,List<SchemaType>> outputSchemas;

    @Singular
    protected List<String> inputNames, outputNames;

    @Singular
    protected Map<String, List<String>> inputColumnNames, outputColumnNames;

    public BasePipelineStep(Map<String, List<SchemaType>> inputSchemas, Map<String, List<SchemaType>> outputSchemas, List<String> inputNames, List<String> outputNames, Map<String, List<String>> inputColumnNames, Map<String, List<String>> outputColumnNames) {
        this.inputSchemas = inputSchemas;
        this.outputSchemas = outputSchemas;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        this.inputColumnNames = inputColumnNames;
        this.outputColumnNames = outputColumnNames;
        initSchemasAndColumnsIfNeeded();
    }


    protected  void initSchemasAndColumnsIfNeeded() {
        if(!(SchemaTypeUtils.allIsNullOrEmpty(inputNames) && SchemaTypeUtils.allIsNullOrEmpty(inputColumnNames) && SchemaTypeUtils.allIsNullOrEmpty(inputSchemas))) {
            Set<String> namesTest = new HashSet<>(inputNames);
            if(SchemaTypeUtils.anyIsNullOrEmpty(inputColumnNames,inputSchemas)) {
                inputColumnNames = new LinkedHashMap<>();
                inputSchemas = new LinkedHashMap<>();
                inputNames.forEach(inputName -> {
                    inputColumnNames.put(inputName, Arrays.asList("default"));
                    inputSchemas.put(inputName, Collections.singletonList(SchemaType.NDArray));
                });
            }
            Preconditions.checkState(namesTest.equals(inputSchemas.keySet()) && namesTest.equals(inputColumnNames.keySet()),"Input schema types, input column names, and input names specified are not consistent!");
        }
        else if(SchemaTypeUtils.anyIsNullOrEmpty(inputColumnNames,inputSchemas)) {
            inputColumnNames = new LinkedHashMap<>();
            inputSchemas = new LinkedHashMap<>();
            log.info("Auto initializing inputs with default column name default and default column type NDArray");
            inputNames.forEach(inputName -> {
                inputColumnNames.put(inputName, Arrays.asList("default"));
                inputSchemas.put(inputName, Collections.singletonList(SchemaType.NDArray));
            });
        }

        else { //initialize all default values
            log.info("No input names or column names or types found. Initializing with default name of default, default column name of default and NDArray type");
            this.inputNames = new ArrayList<>(Arrays.asList("default"));
            this.inputSchemas = new LinkedHashMap<>();
            this.inputSchemas.put("default",Collections.singletonList(SchemaType.NDArray));
            this.inputColumnNames = new LinkedHashMap<>();
            this.inputColumnNames.put("default",Arrays.asList("default"));
        }


        if(!(SchemaTypeUtils.allIsNullOrEmpty(this.outputNames) && SchemaTypeUtils.allIsNullOrEmpty(this.outputSchemas) && SchemaTypeUtils.allIsNullOrEmpty(this.outputColumnNames))) {
            Set<String> outputNamesTest = new HashSet<>(this.outputNames);
            if(SchemaTypeUtils.anyIsNullOrEmpty(outputSchemas,outputColumnNames)) {
                outputColumnNames = new LinkedHashMap<>();
                outputSchemas = new LinkedHashMap<>();
                outputNames.forEach(inputName -> {
                    outputColumnNames.put(inputName, Arrays.asList("default"));
                    outputSchemas.put(inputName, Collections.singletonList(SchemaType.NDArray));
                });
            }
            Preconditions.checkState(this.outputSchemas.keySet().equals(this.outputColumnNames.keySet())
                            && this.outputSchemas.keySet().equals(outputNamesTest),
                    "Output schemas and input column name names are not consistent!");
        } else if(SchemaTypeUtils.anyIsNullOrEmpty(outputSchemas,outputColumnNames)) {
            outputColumnNames = new LinkedHashMap<>();
            outputSchemas = new LinkedHashMap<>();
            log.info("Auto initializing outputs with default column name default and default column type NDArray");
            outputNames.forEach(inputName -> {
                outputColumnNames.put(inputName, Arrays.asList("default"));
                outputSchemas.put(inputName, Collections.singletonList(SchemaType.NDArray));
            });
        } else { //initialize all default values
            this.outputNames = new ArrayList<>(Arrays.asList("default"));
            this.outputSchemas = new LinkedHashMap<>();
            this.outputSchemas.put("default",Collections.singletonList(SchemaType.NDArray));
            this.outputColumnNames = new LinkedHashMap<>();
            this.outputColumnNames.put("default",Arrays.asList("default"));
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public PredictionType[] validPredictionTypes() {
        return new PredictionType[] {
                PredictionType.RAW
        };
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setInput(String[] columnNames, List<SchemaType> types) throws Exception {
        return setInput("default", columnNames, types);
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setInput(Schema inputSchema) throws Exception {
        return setInput("default", inputSchema);
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setInput(String inputName, String[] columnNames, List<SchemaType> types)
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

            return (T) this;
        } else {
            throw new Exception("Input name " + inputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setInput(String inputName, Schema inputSchema) throws Exception {

        List<String> names = getInputNames();
        if (names == null) {
            names = new ArrayList<>();
        }
        if (!names.contains(inputName)) {

            names.add(inputName);
            this.setInputNames(names);

            List<String> columnNames = SchemaTypeUtils.columnNames(inputSchema);
            setInputColumns(inputName, columnNames);

            List<SchemaType> types = SchemaTypeUtils.typesForSchema(inputSchema);
            setInputTypes(inputName, types);

            return (T) this;
        } else {
            throw new Exception("Input name " + inputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setOutput(String[] columnNames, List<SchemaType> types)
            throws Exception {
        return this.setOutput("default", columnNames, types);
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setOutput(Schema outputSchema) throws Exception {
        return this.setOutput("default", outputSchema);
    }

    /**
     *  {@inheritDoc}
     * @return
     */
    @Override
    public T setOutput(String outputName, String[] columnNames, List<SchemaType> types)
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

            return (T) this;
        } else {
            throw new Exception("Output name " + outputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     *  {@inheritDoc}
     * @return
     */

    @Override
    public T setOutput(String outputName, Schema outputSchema) throws Exception {

        List<String> names = getOutputNames();
        if (names == null) {
            names = new ArrayList<>();
        }

        if (!names.contains(outputName)) {
            names.add(outputName);
            setOutputNames(names);

            List<String> columnNames = SchemaTypeUtils.columnNames(outputSchema);
            setOutputColumns(outputName, columnNames);

            List<SchemaType> types = SchemaTypeUtils.typesForSchema(outputSchema);
            setOutputTypes(outputName, types);

            return (T) this;
        } else {
            throw new Exception("Output name " + outputName + "is already configured for this PipelineStep," +
                    " choose another naming convention for your next step.");
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Schema outputSchemaForName(String name) {
        initSchemasAndColumnsIfNeeded();
      
        if (outputSchemas == null || !outputSchemas.containsKey(name))
            return null;

        return SchemaTypeUtils.toSchema(outputSchemas.get(name),
                outputColumnNames.get(name));
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Schema inputSchemaForName(String name) {
        initSchemasAndColumnsIfNeeded();

        if (inputSchemas == null || !inputSchemas.containsKey(name))
            return null;

        return SchemaTypeUtils.toSchema(inputTypesForName(name),
                inputColumnNames.get(name));
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public List<SchemaType> inputTypesForName(String name) {
        initSchemasAndColumnsIfNeeded();
        return inputSchemas.get(name);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean hasInputName(String name) {
        Preconditions.checkState(!inputNames.isEmpty(), "Input names must not be empty!");
        return inputNames.contains(name);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String inputNameAt(int i) {
        Preconditions.checkState(!inputNames.isEmpty(), "Input names must not be empty!");
        return inputNames.get(i);
    }

    /**
     *  { @inheritDoc }
     */
    @Override
    public boolean processColumn(String name, int index) {
        if (inputColumnNames.isEmpty())
            return true;

        if (!inputColumnNames.containsKey(name)) {
            throw new IllegalStateException("Input column names does not contain " + name);
        }

        String columnNameAtIndex = inputColumnNames.get(name).get(index);
        return inputColumnNames.get(name).contains(columnNameAtIndex);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean inputNameIsValidForStep(String name) {
        int idx = inputNames.indexOf(name);
        return idx >= 0;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String inputNameAtIndex(int idx) {
        return inputNames.get(idx);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public PipelineStepRunner createRunner() {
        try {
            Class<? extends PipelineStepRunner> clazz = (Class<? extends PipelineStepRunner>) Class.forName(this.pipelineStepClazz());
            Constructor constructor = clazz.getConstructor(PipelineStep.class);
            return (PipelineStepRunner) constructor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate pipeline step from class " + this.pipelineStepClazz(), e);
        }
    }


    protected void setInputColumns(String inputName, List<String> columnNames) {
        Map<String, List<String>> inputCols = this.getInputColumnNames();
        if (inputCols == null) {
            inputCols = new HashMap<>();
        }
        inputCols.put(inputName, columnNames);
        this.setInputColumnNames(inputCols);
    }

    protected void setOutputColumns(String outputName, List<String> columnNames) {
        Map<String, List<String>> outputCols = this.getOutputColumnNames();
        if (outputCols == null) {
            outputCols = new HashMap<>();
        }
        outputCols.put(outputName, columnNames);
        this.setOutputColumnNames(outputCols);
    }

    protected void setInputTypes(String inputName, List<SchemaType> types) {
        Map<String, List<SchemaType>> schemas = this.getInputSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
        }
        schemas.put(inputName, types);
        this.setInputSchemas(schemas);
    }

    protected void setOutputTypes(String outputName, List<SchemaType> types) {
        Map<String, List<SchemaType>> schemas = this.getOutputSchemas();
        if (schemas == null) {
            schemas = new HashMap<>();
        }
        schemas.put(outputName, types);
        setOutputSchemas(schemas);
    }
}
