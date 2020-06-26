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
package ai.konduit.serving.models.samediff.step;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import org.nd4j.common.base.Preconditions;

public class SameDiffPipelineStepRunnerFactory implements PipelineStepRunnerFactory {


    @Override
    public boolean canRun(PipelineStep pipelineStep) {
        return pipelineStep instanceof SameDiffStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep pipelineStep) {
        Preconditions.checkState(canRun(pipelineStep), "Unable to run pipeline step: %s", pipelineStep.getClass());

        SameDiffStep ps = (SameDiffStep)pipelineStep;
        return new SameDiffRunner(ps);
    }
}
