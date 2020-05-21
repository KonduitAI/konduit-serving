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
package ai.konduit.serving.pipeline.api.step;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;

import java.io.Closeable;


/**
 * PipelineStepRunner executes a given {@link PipelineStep}.
 * Note that PipelineStepRunner instances are instantiated by a {@link PipelineStepRunnerFactory}
 */
public interface PipelineStepRunner extends Closeable {

    /**
     * Destroy the pipeline step runner.
     * <p>
     * This means cleaning up any used resources such as memory, resources, etc.
     * This method will be called when a pipeline needs to be finalized.
     */
    void close();

    /**
     * @return The pipeline step (configuration) that this PipelineStepRunner will execute
     */
    PipelineStep getPipelineStep();

    /**
     * Execute the pipeline on the specified Data instance
     */
    Data exec(Context ctx, Data data);

    /**
     * Execute the pipeline on all of the specified Data instances
     */
    default Data[] exec(Context ctx, Data... data) {
        Data[] out = new Data[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = exec(ctx, data[i]);
        }
        return out;
    }

    /**
     * Get name of the current runner for logging
     */
    default String name(){
        return getClass().getSimpleName();
    }
}