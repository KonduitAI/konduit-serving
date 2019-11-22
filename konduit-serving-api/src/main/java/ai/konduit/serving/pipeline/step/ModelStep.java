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

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.config.NormalizationConfig;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.schema.Schema;

@SuperBuilder
@Data
public class ModelStep extends PipelineStep {

    private ModelConfig modelConfig;

    @Builder.Default
    private ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

    private NormalizationConfig normalizationConfig;

    public ModelStep() {}

    public ModelStep(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    @Override
    public ModelStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelStep) super.setInput("default", columnNames, types);
    }

    @Override
    public ModelStep setOutput(String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelStep) super.setOutput("default", columnNames, types);
    }

    @Override
    public ModelStep setInput(Schema inputSchema) throws Exception {
        return (ModelStep) super.setInput("default", inputSchema);
    }

    @Override
    public ModelStep setOutput(Schema outputSchema) throws Exception {
        return (ModelStep) super.setOutput("default", outputSchema);
    }

    @Override
    public ModelStep setInput(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelStep) super.setInput(inputName, columnNames, types);
    }

    @Override
    public ModelStep setOutput(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (ModelStep) super.setOutput(outputName, columnNames, types);
    }

    @Override
    public ModelStep setInput(String inputName, Schema inputSchema) throws Exception {
        return (ModelStep) super.setInput(inputName, inputSchema);
    }

    @Override
    public ModelStep setOutput(String outputName, Schema outputSchema) throws Exception {
        return (ModelStep) super.setOutput(outputName, outputSchema);
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.InferenceExecutionerStepRunner";
    }
}
