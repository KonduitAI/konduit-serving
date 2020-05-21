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
package ai.konduit.serving.vertx.protocols.http.test;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.protocols.http.InferenceVerticleHttpTest;
import lombok.AllArgsConstructor;

import java.util.Collections;

@AllArgsConstructor
public class FailureTestingPipelineStep implements PipelineStep {

    static {
        PipelineRegistry.registerStepRunnerFactory(new Factory());
        ObjectMappers.registerSubtypes(Collections.singletonList(new JsonSubType("FAILURE_TESTING", FailureTestingPipelineStep.class, PipelineStep.class)));
    }

    public static class Factory implements PipelineStepRunnerFactory {

        @Override
        public boolean canRun(PipelineStep pipelineStep) {
            return pipelineStep instanceof FailureTestingPipelineStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep pipelineStep) {
            return new Runner((FailureTestingPipelineStep)pipelineStep);
        }
    }

    @AllArgsConstructor
    public static class Runner implements PipelineStepRunner {
        private FailureTestingPipelineStep step;

        @Override
        public void close() {

        }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }

        @Override
        public Data exec(Context ctx, Data data) {
            if(InferenceVerticleHttpTest.causeFailure.get())
                throw new IllegalStateException("Intentionally causing a pipeline execution failure!");
            return data;
        }
    }
}
