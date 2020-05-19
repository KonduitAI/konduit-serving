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

import ai.konduit.serving.pipeline.api.context.PipelineProfiler;
import ai.konduit.serving.pipeline.api.context.Profiler;
import ai.konduit.serving.pipeline.api.context.ProfilerConfig;
import ai.konduit.serving.pipeline.api.context.TraceEvent;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.util.CallbackPipelineStep;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.slf4j.event.Level;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class PipelineProfilerTest {
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testProfilingEvents() throws IOException, InterruptedException {

        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();
        Pipeline p = SequencePipeline.builder()
                .add(new CallbackPipelineStep(d -> count1.getAndIncrement()))
                .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO).build())
                .add(new CallbackPipelineStep(d -> count2.getAndIncrement()))
                .build();

        PipelineExecutor pe = p.executor();

        Data d = Data.singleton("someDouble", 1.0);
        d.put("someKey", "someValue");

        File dir = testDir.newFolder();
        File logFile = new File(dir, "test.json");
        System.out.println(logFile.toPath().toString());
        ProfilerConfig profilerConfig = new ProfilerConfig();
        profilerConfig.setOutputFile(Paths.get(logFile.toURI()));
        Profiler profiler = new PipelineProfiler(profilerConfig);
        pe.profilerConfig(profiler);
        pe.exec(d);
        TraceEvent[] events = ((PipelineProfiler)profiler).readEvents(logFile);
        assertEquals(5, events.length);
        assertEquals("Runner", events[0].getName());
        assertEquals("Runner", events[1].getName());
        assertEquals("LoggingPipelineStepRunner", events[2].getName());
        assertEquals("LoggingPipelineStepRunner", events[3].getName());
        assertEquals("Runner", events[4].getName());
    }

    @Test
    public void testEventsJson() throws JsonProcessingException {
        String content = "[{\"name\":\"Runner\",\"cat\":[\"Step\"],\"ts\":507986437484,\"pid\":20272,\"tid\":1,\"ph\":\"START\"},\n" +
                "{\"name\":\"LoggingPipelineStepRunner\",\"cat\":[\"Step\"],\"ts\":507986438897,\"pid\":20272,\"tid\":1,\"ph\":\"START\"},\n" +
                "{\"name\":\"LoggingPipelineStepRunner\",\"cat\":[\"Step\"],\"ts\":507986538818,\"pid\":20272,\"tid\":1,\"ph\":\"END\"},\n" +
                "{\"name\":\"Runner\",\"cat\":[\"Step\"],\"ts\":507986538851,\"pid\":20272,\"tid\":1,\"ph\":\"START\"}]";
        TraceEvent[] events = new ObjectMapper().readValue(content, TraceEvent[].class);
        assertEquals("Runner", events[0].getName());
        assertEquals("LoggingPipelineStepRunner", events[2].getName());
    }
}
