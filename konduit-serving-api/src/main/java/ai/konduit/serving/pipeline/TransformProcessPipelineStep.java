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
    private Map<String,TransformProcess> transformProcesses;


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
         * just from input/output schema and the actual TransformProcess
         *
         * @param inputSchema DataVec Schema for data input
         * @param outputSchema DataVec Schema for data output
         * @param transformProcess DataVec TransformProcess
         * @throws Exception key error
         */
    public TransformProcessPipelineStep(Schema inputSchema, Schema outputSchema,
                                        TransformProcess transformProcess) throws Exception {
        String defaultName = "default";
        this.input(defaultName, inputSchema);
        this.output(defaultName, outputSchema);
        this.transformProcess(defaultName, transformProcess);
    }

    /**
     * Define a single, named step for a transform process.
     *
     * @param stepName input and output name for this step
     * @param inputSchema DataVec Schema for data input
     * @param outputSchema DataVec Schema for data output
     * @param transformProcess DataVec TransformProcess
     * @return this transform process step
     * @throws Exception key error
     */
    public  TransformProcessPipelineStep step(String stepName, Schema inputSchema, Schema outputSchema,
                                              TransformProcess transformProcess) throws Exception {
        this.input(stepName, inputSchema);
        this.output(stepName, outputSchema);
        this.transformProcess(stepName, transformProcess);

        return this;
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


    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.TransformProcessPipelineStepRunner";
    }
}
