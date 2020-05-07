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
package ai.konduit.serving.pipeline.api.pipeline;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import org.slf4j.Logger;

import java.util.List;

/**
 * A pipeline executor implements the actual execution behind the {@link Data} -> {@link Data} mapping that is defined
 * by a {@link Pipeline}
 *
 * @author Alex Black
 */
public interface PipelineExecutor {

    /**
     * Gen the underlying pipeline that this executor will execute
     */
    Pipeline getPipeline();

    /**
     * Get the runners that this PipelineStepExecuter will use to execute the pipeline steps
     */
    List<PipelineStepRunner> getRunners();

    /**
     * Execute the pipeline on the specified Data instance
     */
    Data exec(Context context, Data data);

    /**
     * Execute the pipeline on all of the specified Data instances
     */
    default Data[] exec(Context context, Data... data) {
        Data[] out = new Data[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = exec(context, data[i]);
        }
        return out;
    }

    /**
     * Close the pipeline executor.
     * This means cleaning up any used resources such as memory, database connections, etc.
     * This method will be called when a pipeline needs to be finalized.
     * In practice the call is usually routed to {@link PipelineStepRunner#close()} for each of the {@link PipelineStepRunner}s
     * returned by {@link #getRunners()}
     */
    default void close() {
        for (PipelineStepRunner r : getRunners()) {
            try {
                r.close();
            } catch (Throwable t) {
                getLogger().error("Error closing PipelineStepRunner", t);
            }
        }
    }

    /**
     * Get the logger used by this PipelineExecutor
     */
    Logger getLogger();
}
