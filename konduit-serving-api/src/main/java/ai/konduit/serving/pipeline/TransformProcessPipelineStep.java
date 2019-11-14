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
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransformProcessPipelineStep extends PipelineStep {

    @Getter
    @Setter
    @Singular
    private Map<String, TransformProcess> transformProcesses;

    @Override
    public TransformProcessPipelineStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.setInput("default", columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep setOutput(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.setOutput("default", columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep setInput(Schema inputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.setInput("default", inputSchema);
    }

    @Override
    public TransformProcessPipelineStep setOutput(Schema outputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.setOutput("default", outputSchema);
    }

    @Override
    public TransformProcessPipelineStep setInput(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.setInput(inputName, columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep setOutput(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.setOutput(outputName, columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep setInput(String inputName, Schema inputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.setInput(inputName, inputSchema);
    }

    @Override
    public TransformProcessPipelineStep setOutput(String outputName, Schema outputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.setOutput(outputName, outputSchema);
    }

    /**
     * Create a TransformProcess Step with default input and output names
     * just from output schema and the actual TransformProcess. The
     * input/initial schema can be inferred from the TransformProcess itself
     *
     * @param transformProcess DataVec TransformProcess
     * @param outputSchema DataVec Schema for data output
     * @throws Exception key error
     */
    public TransformProcessPipelineStep(TransformProcess transformProcess, Schema outputSchema) throws Exception {
        this.step("default", transformProcess, outputSchema);
    }

    /**
     * Define a single, named step for a transform process.
     *
     * @param stepName input and output name for this step
     * @param transformProcess DataVec TransformProcess
     * @param outputSchema DataVec Schema for data output
     * @return this transform process step
     * @throws Exception key error
     */
    public TransformProcessPipelineStep step(String stepName, TransformProcess transformProcess, Schema outputSchema
    ) throws Exception {

        this.setInput(stepName, transformProcess.getInitialSchema());
        this.setOutput(stepName, outputSchema);
        this.transformProcess(stepName, transformProcess);

        return this;
    }

    /**
     * Define a single, named step for a transform process.
     *
     * @param transformProcess DataVec TransformProcess
     * @param outputSchema DataVec Schema for data output
     * @return this transform process step
     * @throws Exception key error
     */
    public TransformProcessPipelineStep step( TransformProcess transformProcess, Schema outputSchema
    ) throws Exception {
        return this.step("default", transformProcess, outputSchema);
    }

    /**
     * Define a transform process for this step
     *
     * @param inputName input name
     * @param transformProcess DataVec transform process
     * @return this transform process step
     */
    public TransformProcessPipelineStep transformProcess(String inputName, TransformProcess transformProcess) {
        if (transformProcesses == null) {
            transformProcesses = new HashMap<>();
        }
        transformProcesses.put(inputName, transformProcess);
        return this;
    }

    /**
     * Define a transform process for this step. The name for this step will be "default"
     *
     * @param transformProcess DataVec transform process
     * @return this transform process step
     */
    public TransformProcessPipelineStep transformProcess(TransformProcess transformProcess) {
        return this.transformProcess("default", transformProcess);
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.TransformProcessPipelineStepRunner";
    }
}