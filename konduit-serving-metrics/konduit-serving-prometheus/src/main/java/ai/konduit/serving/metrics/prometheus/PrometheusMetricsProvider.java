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
package ai.konduit.serving.metrics.prometheus;

import ai.konduit.serving.pipeline.registry.MicrometerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.spi.metrics.MetricsProvider;

public class PrometheusMetricsProvider implements MetricsProvider {
    @Override
    public MeterRegistry getRegistry() {
        return MicrometerRegistry.getRegistry();
    }

    @Override
    public Object getEndpoint() {
        return io.vertx.micrometer.PrometheusScrapingHandler.create();
    }
}
