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
package ai.konduit.serving.pipeline.api.context;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class PipelineTimer implements Timer {
    io.micrometer.core.instrument.Timer mmTimer;

    public PipelineTimer(String id) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        mmTimer = registry.timer(id);
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
    public long stop() {
        return io.micrometer.core.instrument.Timer.start().stop(mmTimer);
    }
}
