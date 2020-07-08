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

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphBuilder;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.pipeline.loop.SimpleLoopTrigger;
import ai.konduit.serving.pipeline.impl.pipeline.loop.TimeLoopTrigger;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.testpipelines.count.CountStep;
import ai.konduit.serving.pipeline.impl.testpipelines.time.TimeStep;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncPipelineTest {

    @Test
    public void testAsyncPipelineSimple() throws Exception {

        CountStep cs = new CountStep();
        Pipeline p = SequencePipeline.builder()
                .add(cs)
                .build();

        Trigger t = new SimpleLoopTrigger();
        int count = -1;
        try {

            AsyncPipeline a = new AsyncPipeline(p, t);
            a.start();

            Thread.sleep(2000);

            count = cs.count;
            System.out.println(count);
            assertTrue(count > 10);
        } finally {
            t.stop();
            Thread.sleep(500);
            count = cs.count;
        }
        //Check that no more executions occur after stop
        Thread.sleep(1000);
        assertEquals(count, cs.count);
    }

    @Test
    public void testSimpleLoopTriggerWithDelay() throws Exception {
        for( int run=0; run<3; run++ ) {
            CountStep cs = new CountStep();
            Pipeline p = SequencePipeline.builder()
                    .add(cs)
                    .add(new TimeStep())
                    .build();

            Trigger t = new SimpleLoopTrigger(500L);


            try (AsyncPipeline a = new AsyncPipeline(p, t)) {
                PipelineExecutor exec = a.executor();

                //In 1000ms at 500ms delay, we should have at most 2 or 3 underlying executions (sleep usually isn't exact and exec takes some time)
                Set<Long> times = new HashSet<>();
                for (int i = 0; i < 10; i++) {
                    Data out = exec.exec(Data.empty());
                    long time = out.getLong("time");
                    Thread.sleep(100);
                    times.add(time);
                }

                int count = cs.getCount();
                //System.out.println("COUNT: " + count);
                assertTrue(count == 2 || count == 3);
                assertTrue(times.size() == 2 || times.size() == 3);
            }
        }
    }


    @Test
    public void testTimeLoopTrigger() throws Exception {
        CountStep cs = new CountStep();
        Pipeline p = SequencePipeline.builder()
                .add(cs)
                .add(new TimeStep())
                .build();

        Trigger t = new TimeLoopTrigger(1, TimeUnit.SECONDS);

        //initialize once to avoid initialization overhead causing timing problems
        try (AsyncPipeline a = new AsyncPipeline(p, t)) {
            a.executor().exec(Data.empty());
        }

        try (AsyncPipeline a = new AsyncPipeline(p, t)) {
            PipelineExecutor exec = a.executor();


            //Should execute on the second, every second
            Set<Long> times = new LinkedHashSet<>();
            for (int i = 0; i < 40; i++) {          //~4 seconds total
                Data out = exec.exec(Data.empty());
                long time = out.getLong("time");
                Thread.sleep(100);
                times.add(time);
            }

            System.out.println(times);
            assertTrue(times.toString(), times.size() >= 3 && times.size() <= 5);

            //Check that all times are very close to every second
            long threshold = 20;    //Every second +- 20ms
            for(long l : times){
                long delta = l % 1000;
                assertTrue(l + ", delta=" + delta, delta >= (1000-threshold) || delta < threshold);
            }
        }
    }

    @Test
    public void testJson(){

        for(Trigger t : new Trigger[]{new SimpleLoopTrigger(1000), new TimeLoopTrigger(1, TimeUnit.MINUTES), new TimeLoopTrigger(1, TimeUnit.MINUTES, 20000)}){

            Pipeline p = SequencePipeline.builder()
                    .add(new LoggingStep())
                    .build();

            Pipeline a1 = new AsyncPipeline(p, t);
            String json = a1.toJson();
            String yaml = a1.toYaml();

            System.out.println(json);

            Pipeline a1j = Pipeline.fromJson(json);
            Pipeline a1y = Pipeline.fromYaml(yaml);

            assertEquals(a1, a1j);
            assertEquals(a1, a1y);

            GraphBuilder b = new GraphBuilder();
            GraphStep s = b.input().then("log", new LoggingStep());

            Pipeline g = b.build(s);

            Pipeline a2 = new AsyncPipeline(g, t);

            String gJson = a2.toJson();
            String gYaml = a2.toYaml();

            Pipeline a2j = Pipeline.fromJson(gJson);
            Pipeline a2y = Pipeline.fromYaml(gYaml);

            assertEquals(a2, a2j);
            assertEquals(a2, a2y);
        }

    }
}
