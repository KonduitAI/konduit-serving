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
package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.pipeline.impl.util.CallbackStep;
import org.junit.Test;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TestPipelineSteps {

    @Test
    public void testLoggingPipeline(){

        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();
        Pipeline p = SequencePipeline.builder()
                .add(new CallbackStep(d -> count1.getAndIncrement()))
                .add(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO))
                .add(new CallbackStep(d -> count2.getAndIncrement()))
                .build();

        PipelineExecutor pe = p.executor();

        Data d = Data.singleton("someDouble", 1.0);
        d.put("someKey", "someValue");

        pe.exec(d);
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
        pe.exec(d);
        assertEquals(2, count1.get());
        assertEquals(2, count2.get());
    }

}
