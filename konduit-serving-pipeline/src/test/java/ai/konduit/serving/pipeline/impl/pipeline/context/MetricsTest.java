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
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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
    }
}
