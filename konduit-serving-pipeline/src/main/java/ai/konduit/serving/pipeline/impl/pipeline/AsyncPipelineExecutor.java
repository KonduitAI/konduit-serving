/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.context.Profiler;
import ai.konduit.serving.pipeline.api.context.ProfilerConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.List;

@Slf4j
public class AsyncPipelineExecutor implements PipelineExecutor {

    protected final AsyncPipeline pipeline;
    protected final Trigger trigger;
    protected final PipelineExecutor underlyingExec;


    public AsyncPipelineExecutor(AsyncPipeline pipeline){
        this.pipeline = pipeline;
        this.trigger = pipeline.trigger();
        this.underlyingExec = pipeline.underlying().executor();

        //Set up trigger callback:
        trigger.setCallback(underlyingExec::exec);
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public List<PipelineStepRunner> getRunners() {
        return underlyingExec.getRunners();
    }

    @Override
    public Data exec(Data data) {
        return trigger.query(data);
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void profilerConfig(ProfilerConfig profilerConfig) {
        underlyingExec.profilerConfig(profilerConfig);
    }

    @Override
    public Profiler profiler() {
        return underlyingExec.profiler();
    }
}
