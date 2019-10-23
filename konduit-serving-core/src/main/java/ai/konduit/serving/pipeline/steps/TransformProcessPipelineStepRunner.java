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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.TransformProcessPipelineStep;
import org.datavec.api.records.Record;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.writable.Writable;
import org.datavec.local.transforms.LocalTransformExecutor;
import org.nd4j.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Run transform processes for each input name.
 */
public class TransformProcessPipelineStepRunner extends BasePipelineStepRunner {

    private Map<String,TransformProcess> transformProcesses;

    public TransformProcessPipelineStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        TransformProcessPipelineStep transformProcessPipelineStepConfig = (TransformProcessPipelineStep) pipelineStep;
        this.transformProcesses = transformProcessPipelineStepConfig.getTransformProcesses();
    }

    @Override
    public Record[] transform(Record[] input) {
        Record[] ret = new Record[input.length];
        for(int i = 0; i < input.length; i++) {
            String inputName = pipelineStep.inputNameAt(i);
            if(pipelineStep.inputNameIsValidForStep(inputName)) {
                TransformProcess toExecute = transformProcesses.get(inputName);
                Preconditions.checkNotNull(toExecute,"No transform process found for name " + inputName);
                ret[i] = new org.datavec.api.records.impl.Record(
                        LocalTransformExecutor.execute(
                                Collections.singletonList(input[i].getRecord()),
                                toExecute).get(0),
                        null);
            } else {
                ret[i] = input[i];
            }
        }
        return ret;
    }

    @Override
    public void processValidWritable(Writable writable, List<Writable> record, int inputIndex, Object... extraArgs) {
           throw new UnsupportedOperationException();
    }
}
