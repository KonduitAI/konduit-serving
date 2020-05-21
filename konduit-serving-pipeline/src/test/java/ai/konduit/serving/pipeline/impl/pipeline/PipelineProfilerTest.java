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

import ai.konduit.serving.pipeline.api.context.*;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.util.CallbackPipelineStep;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import lombok.AllArgsConstructor;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.slf4j.event.Level;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        ProfilerConfig profilerConfig = new ProfilerConfig()
                .outputFile(Paths.get(logFile.toURI()));
        pe.profilerConfig(profilerConfig);
        pe.exec(d);

        //Block until profiler has finished writing current results to file
        pe.profiler().flushBlocking();

        TraceEvent[] events = PipelineProfiler.readEvents(logFile);
        assertEquals(6, events.length);
        assertEquals("Runner", events[0].getName());
        assertEquals("Runner", events[1].getName());
        assertEquals("LoggingPipelineStepRunner", events[2].getName());
        assertEquals("LoggingPipelineStepRunner", events[3].getName());
        assertEquals("Runner", events[4].getName());
    }

    @Test
    public void testProfilingLogRotation() throws IOException {

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
        ProfilerConfig profilerConfig = new ProfilerConfig()
                .outputFile(Paths.get(logFile.toURI()))
                .splitSize(10);
        pe.profilerConfig(profilerConfig);
        for (int i = 0; i < 100; ++i) {
            pe.exec(d);
        }
        // Assume no race condition or I/O failures if we are here.
    }

    @Test
    public void testEventsJson() throws JsonProcessingException {
        String content = "[{\"name\":\"Runner\",\"cat\":\"START\",\"ts\":577532080904,\"pid\":17104,\"tid\":1,\"ph\":\"B\"},\n" +
                "{\"name\":\"Runner\",\"cat\":\"END\",\"ts\":577532194829,\"pid\":17104,\"tid\":1,\"ph\":\"E\"},\n" +
                "{\"name\":\"LoggingPipelineStepRunner\",\"cat\":\"START\",\"ts\":577532194878,\"pid\":17104,\"tid\":1,\"ph\":\"B\"},\n" +
                "{\"name\":\"LoggingPipelineStepRunner\",\"cat\":\"END\",\"ts\":577532195804,\"pid\":17104,\"tid\":1,\"ph\":\"E\"},\n" +
                "{\"name\":\"Runner\",\"cat\":\"START\",\"ts\":577532195829,\"pid\":17104,\"tid\":1,\"ph\":\"B\"}]";
        TraceEvent[] events = new ObjectMapper().readValue(content, TraceEvent[].class);
        assertEquals("Runner", events[0].getName());
        assertEquals("LoggingPipelineStepRunner", events[2].getName());
    }


    @Test
    public void testNonClosed() throws Exception {
        //Test that if steps are opened but not closed, the profiler automatically closes them

        File dir = testDir.newFolder();
        File f = new File(dir, "profiler.json");


        PipelineRegistry.registerStepRunnerFactory(new TestStepFactory());

        Pipeline p = SequencePipeline.builder()
                .add(new TestStep(new String[]{"x", "y"}))
                .add(new TestStep(new String[]{"a", "b", "c"}))
                .build();

        PipelineExecutor exec = p.executor();
        exec.profilerConfig(new ProfilerConfig()
                .outputFile(f));

        Data in = Data.singleton("someKey", "someValue");
        exec.exec(in);

        exec.profiler().flushBlocking();

        //We should have exactly
        String json = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        System.out.println(json);

        //Expected number of lines:
        // 2 steps, start and end -> 4
        // 5 within pipeline starts
        // 5 within pipeline ends (automatically generated because pipeline didn't close them)
        // 14 lines total expected

        String[] lines = json.split("\n");
        assertEquals(14, lines.length);
    }

    @AllArgsConstructor
    private static class TestStep implements PipelineStep {
        private String[] toOpen;
    }

    @AllArgsConstructor
    private static class TestStepRunner implements PipelineStepRunner {

        private final TestStep step;

        @Override
        public void close() { }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }

        @Override
        public Data exec(Context ctx, Data data) {

            Profiler p = ctx.profiler();
            for(String s : step.toOpen){
                p.eventStart(s);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
            }
            //Note: no eventEnd called
            return data;
        }
    }

    private static class TestStepFactory implements PipelineStepRunnerFactory {

        @Override
        public boolean canRun(PipelineStep step) {
            return step instanceof TestStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep step) {
            return new TestStepRunner((TestStep)step);
        }
    }
}
