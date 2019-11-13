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

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.config.ServingConfig;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;

import java.util.HashMap;

@SuperBuilder
@Data
public class ModelPipelineStep extends PipelineStep {

    private ModelConfig modelConfig;

    @Builder.Default
    private ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

    private NormalizationConfig normalizationConfig;

    public ModelPipelineStep() {}

    public ModelPipelineStep(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public ModelPipelineStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelPipelineStep) super.setInput("default", columnNames, types);
    }

    @Override
    public ModelPipelineStep setOutput(String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelPipelineStep) super.setOutput("default", columnNames, types);
    }

    @Override
    public ModelPipelineStep setInput(Schema inputSchema) throws Exception {
        return (ModelPipelineStep) super.setInput("default", inputSchema);
    }

    @Override
    public ModelPipelineStep setOutput(Schema outputSchema) throws Exception {
        return (ModelPipelineStep) super.setOutput("default", outputSchema);
    }

    @Override
    public ModelPipelineStep setInput(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelPipelineStep) super.setInput(inputName, columnNames, types);
    }

    @Override
    public ModelPipelineStep setOutput(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelPipelineStep) super.setOutput(outputName, columnNames, types);
    }

    @Override
    public ModelPipelineStep setInput(String inputName, Schema inputSchema) throws Exception {
        return (ModelPipelineStep) super.setInput(inputName, inputSchema);
    }

    @Override
    public ModelPipelineStep setOutput(String outputName, Schema outputSchema) throws Exception {
        return (ModelPipelineStep) super.setOutput(outputName, outputSchema);
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.InferenceExecutionerPipelineStepRunner";
    }
}
