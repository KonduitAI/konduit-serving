/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.pipeline.api.context;

import ai.konduit.serving.pipeline.registry.MicrometerRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class PipelineTimer implements Timer {
    @Getter
    private io.micrometer.core.instrument.Timer mmTimer;

    public PipelineTimer(String id) {
        mmTimer = MicrometerRegistry.getRegistry().timer(id);
    }

    @Override
    public void record(Duration duration) {
        mmTimer.record(duration);
    }

    @Override
    public void record(long duration, TimeUnit timeUnit) {
        mmTimer.record(duration, timeUnit);
    }

    @Override
    public Timer.Sample start() {
        io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        return new Sample(sample);
    }

    @AllArgsConstructor
    public static class Sample implements Timer.Sample {
        private io.micrometer.core.instrument.Timer.Sample mmSample;

        public long stop(Timer timer) {
            return mmSample.stop(((PipelineTimer)timer).getMmTimer());
        }
    }
}
