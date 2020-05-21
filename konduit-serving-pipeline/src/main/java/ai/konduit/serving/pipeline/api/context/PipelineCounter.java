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

public class PipelineCounter implements Counter {

    private io.micrometer.core.instrument.Counter mmCounter;

    public PipelineCounter(String id) {
        mmCounter = io.micrometer.core.instrument.Counter.builder(id).register(new SimpleMeterRegistry());
    }

    @Override
    public void increment() {
        mmCounter.increment();
    }

    @Override
    public void increment(double value) {
        mmCounter.increment(value);
    }
}
