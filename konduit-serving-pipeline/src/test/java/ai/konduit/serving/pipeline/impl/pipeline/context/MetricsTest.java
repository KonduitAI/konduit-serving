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
package ai.konduit.serving.pipeline.impl.pipeline.context;

import ai.konduit.serving.pipeline.api.context.*;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.util.CallbackPipelineStep;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.event.Level;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetricsTest {

    private Metrics m;

    @Before
    public void setUp() {
        m = new PipelineMetrics();
    }

    @Test
    public void testCounter() {
        Counter counter = m.counter("test");
        counter.increment();
        counter.increment(10.0);
    }

    @Test
    public void testGauge() {
        Gauge gauge = m.gauge("id", 10.0);
        assertEquals(10.0, gauge.value(), 1e-4);
    }

    @Test
    public void testTimer() {
        Timer timer = m.timer("test");
        timer.record(30, TimeUnit.MILLISECONDS);
        long ret = timer.stop();
        assertTrue(ret > 0);
    }

    @Test
    public void testPipelineMetrics() {
        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();

        PipelineStep step1 = new CallbackPipelineStep(d -> count1.getAndIncrement());
        PipelineStep step2 = LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO).build();
        PipelineStep step3 = new CallbackPipelineStep(d -> count2.getAndIncrement());

        Pipeline p = SequencePipeline.builder()
                .add(step1)
                .add(step2)
                .add(step3)
                .build();


        String id = p.id().toString() + "." + step1.toString() + ".test";
        Timer timer = m.timer(id);
        timer.record(30, TimeUnit.MILLISECONDS);
        long ret = timer.stop();
        assertTrue(ret > 0);
    }
}
