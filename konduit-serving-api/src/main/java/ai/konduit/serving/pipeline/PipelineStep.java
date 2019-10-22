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

import ai.konduit.serving.model.MemMapConfig;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.util.SchemaTypeUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.schema.Schema;
import org.nd4j.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id.NAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
/*
 * PipelineStep collects all ETL related properties (input schema,
 * normalization and transform steps, output schema, potential pre-
 * or post-processing etc.). This config is passed to the respective
 * verticle along with Model and Serving configurations.
 */
@SuperBuilder
@JsonSubTypes({
        @JsonSubTypes.Type(value= ImageLoading.class, name = "ImageLoading"),
        @JsonSubTypes.Type(value= MemMapConfig.class, name = "MemMapConfig"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= NormalizationConfig.class, name = "NormalizationConfig"),
        @JsonSubTypes.Type(value= ObjectDetectionConfig.class, name = "ObjectDetectionConfig"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= PythonPipelineStep.class, name = "PythonPipelineStep"),
        @JsonSubTypes.Type(value= ModelPipelineStep.class, name = "ModelPipelineStep"),
        @JsonSubTypes.Type(value= TransformProcessPipelineStep.class, name = "TransformProcessPipelineStep"),
        @JsonSubTypes.Type(value= ArrayConcatenationStep.class, name = "ArrayConcatenationStep"),
        @JsonSubTypes.Type(value= PmmlPipelineStep.class, name = "PmmlPipelineStep"),
        @JsonSubTypes.Type(value= CustomPipelineStep.class, name = "CustomPipelineStep")
})
@JsonTypeInfo(use = NAME, include = PROPERTY)
@EqualsAndHashCode(callSuper = false)
public abstract class PipelineStep implements Serializable {

    @Singular
    private Map<String, SchemaType[]> inputSchemas;
    @Singular
    private Map<String, SchemaType[]> outputSchemas;

    @Singular
    private List<String> inputNames;
    @Singular
    private List<String> outputNames;

    //only process these names
    @Singular
    private List<String> affectedInputNames, affectedOutputNames;
    @Singular
    private Map<String, List<String>> inputColumnNames, outputColumnNames;

    public List<String> getTargetInputStepInputNames() {
        return affectedInputNames != null ? affectedInputNames : inputNames;
    }

    public Schema inputSchemaForName(String name) {
        Preconditions.checkNotNull(inputSchemas,"No input schemas specified in configuration!");
        if(!inputSchemas.containsKey(name))
            return null;

        return SchemaTypeUtils.toSchema(inputTypesForName(name),
                inputColumnNames.get(name));
    }

    public Schema outputSchemaForName(String name) {
        Preconditions.checkNotNull(outputSchemas,"No output schemas specified in configuration!");

        if(!outputSchemas.containsKey(name))
            return null;

        return SchemaTypeUtils.toSchema(outputSchemas.get(name), outputColumnNames.get(name));
    }

    public SchemaType[] inputTypesForName(String name) {
        if(!inputSchemas.containsKey(name))
            return null;

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

    public int indexOfInputInputName(String inputName) {
        return inputNames.indexOf(inputName);
    }

    public List<String> getTargetInputStepOutputNames() {
        return affectedOutputNames != null ? affectedOutputNames : outputNames;
    }

    public boolean inputNameIsValidForStep(String name) {
        int idx = inputNames.indexOf(name);
        return idx >= 0;
    }

    public String inputNameAtIndex(int idx) {
        return inputNames.get(idx);
    }

    public abstract String pipelineStepClazz();

    public enum StepType {
        PYTHON,
        MODEL,
        TRANSFORM,
        NORMALIZATION,
        IMAGE_LOADING,
        OBJECT_DETECTION
    }



}
