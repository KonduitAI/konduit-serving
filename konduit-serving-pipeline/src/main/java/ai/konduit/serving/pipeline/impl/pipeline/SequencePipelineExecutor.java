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
package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.context.*;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.context.DefaultContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * An executor for {@link SequencePipeline}s
 *
 * @author Alex Black
 */
@Slf4j
public class SequencePipelineExecutor extends BasePipelineExecutor {

    private SequencePipeline pipeline;
    private List<PipelineStepRunner> runners;
    private ProfilerConfig profilerConfig;
    private Profiler profiler = new NoOpProfiler();
    private Metrics metrics;
    private Context ctx;

    public SequencePipelineExecutor(@NonNull SequencePipeline p) {
        this.pipeline = p;

        //Initialize
        runners = new ArrayList<>();
        List<PipelineStep> steps = p.steps();

        for (PipelineStep ps : steps) {
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

    @Override
    public Data exec(Data data) {
        if (ctx == null) {
            metrics = new PipelineMetrics(pipeline.id());
            ctx = new DefaultContext(metrics, profiler);
        }

        Data current = data;
        for (PipelineStepRunner psr : runners) {
            String name = psr.name();
            profiler.eventStart(name);
            ((PipelineMetrics)metrics).setInstanceName(name);
            ((PipelineMetrics)metrics).setStepName(psr.getPipelineStep().name());

            current = psr.exec(ctx, current);

            profiler.eventEnd(name);

            //Ensure that the step didn't open but not close any profiles
            profiler.closeAll();
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
        if (profilerConfig != null) {
            this.profiler = new PipelineProfiler(profilerConfig);
        } else {
            this.profiler = new NoOpProfiler();
        }
    }

    @Override
    public Profiler profiler() {
        return profiler;
    }
}
