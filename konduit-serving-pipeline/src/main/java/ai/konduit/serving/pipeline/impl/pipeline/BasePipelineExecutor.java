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
package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.common.base.Preconditions;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BasePipelineExecutor implements PipelineExecutor {


    PipelineStepRunner getRunner(@NonNull PipelineStep step) {

        List<PipelineStepRunnerFactory> factories = PipelineRegistry.getStepRunnerFactories();

        PipelineStepRunnerFactory f = null;
        for (PipelineStepRunnerFactory psrf : factories) {
            if (psrf.canRun(step)) {
                if (f != null) {
                    log.warn("TODO - Multiple PipelineStepRunnerFactory instances can run pipeline {} - {} and {}", step.getClass(), f.getClass(), psrf.getClass());
                }

                f = psrf;
                //TODO make debug level later
                //log.info("PipelineStepRunnerFactory {} used to run step {}", psrf.getClass().getName(), ps.getClass().getName());
            }
        }

        if (f == null) {
            StringBuilder msg = new StringBuilder("Unable to execute pipeline step of type " + step.getClass().getName() + ": No PipelineStepRunnerFactory instances"
                    + " are available that can execute this pipeline step.\nThis likely means a required dependency is missing for executing this pipeline." +
                    "\nAvailable executor factories:");
            if (factories.isEmpty()) {
                msg.append(" <None>");
            }
            boolean first = true;
            for (PipelineStepRunnerFactory psrf : factories) {
                if (!first)
                    msg.append("\n");
                msg.append("  ").append(psrf.getClass().getName());

                first = false;
            }
            throw new IllegalStateException(msg.toString());
        }

        PipelineStepRunner r = f.create(step);
        Preconditions.checkNotNull(r, "Failed to create PipelineStepRunner: PipelineStepRunnerFactory.create(...) returned null: " +
                "Pipeline step %s, PipelineStepRunnerFactory %s", step.getClass(), f.getClass());
        return r;
    }
}
