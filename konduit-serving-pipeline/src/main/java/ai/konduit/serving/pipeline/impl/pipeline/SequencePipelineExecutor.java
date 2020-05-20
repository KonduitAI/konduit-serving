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
import ai.konduit.serving.pipeline.api.context.PipelineProfiler;
import ai.konduit.serving.pipeline.api.context.Profiler;
import ai.konduit.serving.pipeline.api.context.ProfilerConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nd4j.common.base.Preconditions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SequencePipelineExecutor extends BasePipelineExecutor {

    private SequencePipeline pipeline;
    private List<PipelineStepRunner> runners;
    private ProfilerConfig profilerConfig;

    public SequencePipelineExecutor(@NonNull SequencePipeline p){
        this.pipeline = p;

        //Initialize
        runners = new ArrayList<>();
        List<PipelineStep> steps = p.getSteps();

        for(PipelineStep ps : steps){
            PipelineStepRunner r = getRunner(ps);
            runners.add(r);
        }
    }


    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public List<PipelineStepRunner> getRunners() {
        return runners;
    }

	// TODO: review profiler lifetime
    private static Profiler profiler;
    private static Profiler createProfiler(ProfilerConfig profilerConfig) {
        if (profiler == null) {
            profiler = new PipelineProfiler(profilerConfig);
        }
        return profiler;
    }

    @Override
    public Data exec(Data data) {
        Context ctx = null; //TODO
        Data current = data;
        String saved = StringUtils.EMPTY;
        Profiler profiler = createProfiler(profilerConfig);
        for (PipelineStepRunner psr : runners) {
           if (profiler.profilerEnabled()) {
              saved = psr.name();
              profiler.eventStart(saved);
           }
           current = psr.exec(ctx, current);
           if (profiler.profilerEnabled()) {
               profiler.eventEnd(psr.name());
           }
        }
        if (profiler.profilerEnabled() && ((PipelineProfiler) profiler).isLogActive()) {
          profiler.eventEnd(saved);
        }
        return current;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void profilerConfig(ProfilerConfig profilerConfig) {
        this.profilerConfig = profilerConfig;
    }
}
