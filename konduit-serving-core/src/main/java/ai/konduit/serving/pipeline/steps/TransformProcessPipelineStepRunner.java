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
import ai.konduit.serving.pipeline.step.TransformProcessStep;
import org.datavec.api.records.Record;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.writable.Writable;
import org.datavec.local.transforms.LocalTransformExecutor;
import org.nd4j.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Run transform processes for each input name.
 * Using datavec's {@link LocalTransformExecutor}
 * we run a {@link TransformProcess} on each specified input.
 * Generally this will just be run on the "default"
 * input name.
 * A {@link TransformProcess} is typically for columnar data
 * but can be used for binary data as well.
 *
 * @author Adam Gibson
 */
public class TransformProcessStepRunner extends BasePipelineStepRunner {

    private Map<String,TransformProcess> transformProcesses;

    public TransformProcessStepRunner(PipelineStep pipelineStep) {
        super(pipelineStep);
        TransformProcessStep transformProcessStepConfig = (TransformProcessStep) pipelineStep;
        this.transformProcesses = transformProcessStepConfig.getTransformProcesses();
    }



    @Override
    public Record[] transform(Record[] input) {
        Record[] ret = new Record[input.length];
        for(int i = 0; i < input.length; i++) {
            if(pipelineStep.inputNameIsValidForStep(pipelineStep.inputNameAt(i))) {
                TransformProcess toExecute = transformProcesses.get(pipelineStep.inputNameAt(i));
                Preconditions.checkNotNull(toExecute,"No transform process found for name " + (pipelineStep.inputNameAt(i)));
                ret[i] = new org.datavec.api.records.impl.Record(LocalTransformExecutor.execute(Arrays.asList(input[i].getRecord()),toExecute).get(0),null);

            }
            else {
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
