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

import ai.konduit.serving.pipeline.registry.MicrometerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class PipelineGauge implements Gauge {

    private io.micrometer.core.instrument.Gauge mmGauge;

    public PipelineGauge(String id, double value) {
        mmGauge =  io.micrometer.core.instrument.Gauge.builder(id, value, new ToDoubleFunction<Double>() {
            @Override
            public double applyAsDouble(Double value) {
                return value;
            }
        }).register(MicrometerRegistry.getRegistry());
    }

    public PipelineGauge(String id, List<?> list) {
        mmGauge = io.micrometer.core.instrument.Gauge.builder(id, list, List::size).register(new SimpleMeterRegistry());
    }

    public double value() {
        return mmGauge.value();
    }
}
