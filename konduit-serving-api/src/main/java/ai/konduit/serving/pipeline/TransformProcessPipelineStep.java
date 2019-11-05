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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.datavec.api.transform.TransformProcess;

import java.util.*;

import lombok.Singular;
import org.datavec.api.transform.schema.Schema;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransformProcessPipelineStep extends PipelineStep {

    @Getter
    @Setter
    @Singular
    private Map<String, TransformProcess> transformProcesses;

    public TransformProcessPipelineStep input(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.input("default", columnNames, types);
    }

    public TransformProcessPipelineStep output(String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.output("default", columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep input(Schema inputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.input("default", inputSchema);
    }

    @Override
    public TransformProcessPipelineStep output(Schema outputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.output("default", outputSchema);
    }

    public TransformProcessPipelineStep input(String inputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.input(inputName, columnNames, types);
    }

    public TransformProcessPipelineStep output(String outputName, String[] columnNames, SchemaType[] types) throws Exception {
        return (TransformProcessPipelineStep) super.output(outputName, columnNames, types);
    }

    @Override
    public TransformProcessPipelineStep input(String inputName, Schema inputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.input(inputName, inputSchema);
    }

    @Override
    public TransformProcessPipelineStep output(String outputName, Schema outputSchema) throws Exception {
        return (TransformProcessPipelineStep) super.output(outputName, outputSchema);
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

        this.input(stepName, transformProcess.getInitialSchema());
        this.output(stepName, outputSchema);
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
