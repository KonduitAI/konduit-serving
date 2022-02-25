/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.pipeline.impl.util;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import lombok.*;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.function.Consumer;

@lombok.Data
public class CallbackStep implements PipelineStep {


    public CallbackStep(@JsonProperty("consumer") Consumer<Data> consumer){
        this.consumer = consumer;
    }

    static {
        PipelineRegistry.registerStepRunnerFactory(new Factory());
    }


    private Consumer<Data> consumer;


    public static class Factory implements PipelineStepRunnerFactory {

        @Override
        public boolean canRun(PipelineStep pipelineStep) {
            return pipelineStep instanceof CallbackStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep pipelineStep) {
            return new Runner((CallbackStep)pipelineStep);
        }
    }

    @AllArgsConstructor
    public static class Runner implements PipelineStepRunner {
        private CallbackStep step;

        @Override
        public void close() {

        }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }

        @Override
        public Data exec(Context ctx, Data data) {
            step.consumer.accept(data);
            return data;
        }
    }
}
