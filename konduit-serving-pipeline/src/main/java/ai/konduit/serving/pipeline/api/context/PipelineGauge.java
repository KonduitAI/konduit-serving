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
import org.nd4j.common.primitives.AtomicDouble;

public class PipelineGauge implements Gauge {

    private io.micrometer.core.instrument.Gauge mmGauge;
    private AtomicDouble d;

    public PipelineGauge(String id, double value) {
        d = new AtomicDouble(value);
        mmGauge = io.micrometer.core.instrument.Gauge
                .builder(id, d, AtomicDouble::get)
                .register(MicrometerRegistry.getRegistry());
    }

    public void set(double set){
        d.set(set);
    }

    public double value() {
        return mmGauge.value();
    }
}
