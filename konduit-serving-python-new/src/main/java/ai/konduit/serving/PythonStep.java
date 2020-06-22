/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/


package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import lombok.experimental.Accessors;
import org.nd4j.python4j.PythonJob;
import org.nd4j.python4j.PythonVariable;
import org.nd4j.common.base.Preconditions;

import java.util.Collections;
import java.util.UUID;

@lombok.Data
@Accessors(fluent=true)
public class PythonStep implements PipelineStep {

    private String code;
    private String setupMethod;
    private String runMethod;

    public static class Factory implements PipelineStepRunnerFactory{

        @Override
        public boolean canRun(PipelineStep pipelineStep){
            return pipelineStep instanceof PythonStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep pipelineStep){
            Preconditions.checkState(canRun(pipelineStep), "Required PythonStep. Received " + pipelineStep.getClass());
            return new Runner((PythonStep) pipelineStep);
        }

    }

    public static class Runner implements PipelineStepRunner{

        private final PythonStep pythonStep;
        private final PythonJob pythonJob;

        private  String resolveActualCode(String userCode, String setupF, String runF){
            StringBuilder sb = new StringBuilder();
            sb.append(userCode);
            if (!setupF.equals("setup")){
                sb.append("\n").append("setup = ").append(setupF).append("\n");
            }
            if (runF.equals("run")){
                sb.append("\n").append("runOrig = ").append("run").append("\n");
                runF = "runOrig";
            }

            sb.append("\n").append("run = lambda input: {'output':").append(runF).append("(input)").append("}");
            return sb.toString();
        }

        public Runner(PythonStep pythonStep){
            this.pythonStep = pythonStep;
            this.pythonJob = new PythonJob("job_" + UUID.randomUUID().toString().replace('-', '_'),
                    resolveActualCode(pythonStep.code, pythonStep.setupMethod, pythonStep.runMethod), true);
        }

        @Override
        public Data exec(Context ctx, Data input){
            PythonVariable<Data> pyInput = new PythonVariable<>("input", PyData.INSTANCE, input);
            PythonVariable<Data> pyOutput = new PythonVariable<>("output", PyData.INSTANCE, null);
            pythonJob.exec(Collections.singletonList(pyInput), Collections.singletonList(pyOutput));
            return pyOutput.getValue();
        }

        @Override
        public PipelineStep getPipelineStep(){
            return pythonStep;
        }

        @Override
        public void close() {}
    }
}
