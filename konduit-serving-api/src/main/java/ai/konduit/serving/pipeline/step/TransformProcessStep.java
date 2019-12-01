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
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransformProcessStep extends BasePipelineStep {

    @Getter
    @Setter
    @Singular
    private Map<String, TransformProcess> transformProcesses;

    /**
     * Create a {@link TransformProcess} Step with default input and output names
     * just from output schema and the actual {@link TransformProcess}. The
     * input/initial schema can be inferred from the TransformProcess itself
     *
     * @param transformProcess DataVec TransformProcess
     * @param outputSchema     DataVec Schema for data output
     * @throws Exception key error
     */
    public TransformProcessStep(TransformProcess transformProcess, Schema outputSchema) throws Exception {
        this.step("default", transformProcess, outputSchema);
    }

    @Override
    public DataFormat[] validInputTypes() {
        return new DataFormat[] {
                DataFormat.JSON,
                DataFormat.ARROW,
                DataFormat.NUMPY,
                DataFormat.ND4J
        };
    }

    @Override
    public Output.DataFormat[] validOutputTypes() {
        return new Output.DataFormat[] {
                Output.DataFormat.ND4J,
                Output.DataFormat.ARROW,
                Output.DataFormat.NUMPY,
                Output.DataFormat.JSON
        };
    }

    @Override
    public PipelineStep setInput(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessStep) super.setInput("default", columnNames, types);
    }

    @Override
    public PipelineStep setOutput(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessStep) super.setOutput("default", columnNames, types);
    }

    @Override
    public PipelineStep setInput(Schema inputSchema) throws Exception {
        return (TransformProcessStep) super.setInput("default", inputSchema);
    }

    @Override
    public PipelineStep setOutput(Schema outputSchema) throws Exception {
        return (TransformProcessStep) super.setOutput("default", outputSchema);
    }

    @Override
    public PipelineStep setInput(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessStep) super.setInput(inputName, columnNames, types);
    }

    @Override
    public PipelineStep setOutput(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessStep) super.setOutput(outputName, columnNames, types);
    }

    @Override
    public PipelineStep setInput(String inputName, Schema inputSchema) throws Exception {
        return (TransformProcessStep) super.setInput(inputName, inputSchema);
    }

    @Override
    public PipelineStep setOutput(String outputName, Schema outputSchema) throws Exception {
        return (TransformProcessStep) super.setOutput(outputName, outputSchema);
    }

    /**
     * Define a single, named step for a transform process.
     *
     * @param stepName         input and output name for this step
     * @param transformProcess DataVec TransformProcess
     * @param outputSchema     DataVec Schema for data output
     * @return this transform process step
     * @throws Exception key error
     */
    public TransformProcessStep step(String stepName, TransformProcess transformProcess, Schema outputSchema
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
     * @param outputSchema     DataVec Schema for data output
     * @return this transform process step
     * @throws Exception key error
     */
    public TransformProcessStep step(TransformProcess transformProcess, Schema outputSchema
    ) throws Exception {
        return this.step("default", transformProcess, outputSchema);
    }

    /**
     * Define a transform process for this step
     *
     * @param inputName        input name
     * @param transformProcess DataVec transform process
     * @return this transform process step
     */
    public TransformProcessStep transformProcess(String inputName, TransformProcess transformProcess) {
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
    public TransformProcessStep transformProcess(TransformProcess transformProcess) {
        return this.transformProcess("default", transformProcess);
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.TransformProcessStepRunner";
    }
}