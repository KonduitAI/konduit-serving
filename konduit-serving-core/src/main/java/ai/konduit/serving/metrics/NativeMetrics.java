/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.metrics;

import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.config.metrics.MetricsRenderer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.deeplearning4j.perf.listener.DeviceMetric;
import org.deeplearning4j.perf.listener.HardwareMetric;
import oshi.json.SystemInfo;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * Metrics derived from
 * {@link HardwareMetric}
 * which contains current information about the system and its devices
 * such as ram, cpu load, and gpu information
 */
public class NativeMetrics implements MetricsRenderer {

    private final Iterable<Tag> tags;

    public NativeMetrics() {
        this(emptyList());
    }

    public NativeMetrics(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        SystemInfo systemInfo = new SystemInfo();
        HardwareMetric hardwareMetric = HardwareMetric.fromSystem(systemInfo, UUID.randomUUID().toString());
        Gauge.builder("cpuload", hardwareMetric, HardwareMetric::getAveragedCpuLoad)
                .tags(tags)
                .description("Average cpu load")
                .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                .register(registry);


        Gauge.builder("memoryuse", hardwareMetric, HardwareMetric::getCurrentMemoryUse)
                .tags(tags)
                .description("Memory use")
                .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                .register(registry);


        Gauge.builder("iowaittime", hardwareMetric, HardwareMetric::getIoWaitTime)
                .tags(tags)
                .description("I/O Wait time")
                .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                .register(registry);


        if (hardwareMetric.getGpuMetrics() != null)
            for (Map.Entry<Integer, DeviceMetric> entry : hardwareMetric.getGpuMetrics().entrySet()) {
                DeviceMetric deviceMetric = hardwareMetric.getGpuMetrics().get(entry.getKey());
                Gauge.builder("gpu." + entry.getKey() + ".bandwidth.d2d" + entry.getKey(), deviceMetric, DeviceMetric::getBandwidthDeviceToDevice)
                        .tags(tags)
                        .description("Gpu " + entry.getKey() + " bandwidth device to device for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);

                Gauge.builder("gpu." + entry.getKey() + ".bandwidth.d2h" + entry.getKey(), deviceMetric, DeviceMetric::getBandwidthDeviceToHost)
                        .tags(tags)
                        .description("Gpu " + entry.getKey() + " bandwidth device to host for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);

                Gauge.builder("gpu." + entry.getKey() + ".load" + entry.getKey(), deviceMetric, DeviceMetric::getLoad)
                        .tags(tags)
                        .description("Gpu " + entry.getKey() + " current load for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);

                Gauge.builder("gpu." + entry.getKey() + ".memavailable" + entry.getKey(), deviceMetric, DeviceMetric::getMemAvailable)
                        .tags(tags)
                        .description("Gpu " + entry.getKey() + " current available memory for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);


            }


        if (hardwareMetric.getPerCoreMetrics() != null)
            for (Map.Entry<Integer, DeviceMetric> entry : hardwareMetric.getPerCoreMetrics().entrySet()) {
                DeviceMetric deviceMetric = hardwareMetric.getPerCoreMetrics().get(entry.getKey());
                Gauge.builder("Cpu." + entry.getKey() + ".load" + entry.getKey(), deviceMetric, DeviceMetric::getLoad)
                        .tags(tags)
                        .description("Cpu " + entry.getKey() + " current load for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);

                Gauge.builder("cpu." + entry.getKey() + ".memavailable" + entry.getKey(), deviceMetric, DeviceMetric::getMemAvailable)
                        .tags(tags)
                        .description("Cpu " + entry.getKey() + " current available memory for device " + deviceMetric.getDeviceName())
                        .baseUnit("konduit-serving." + hardwareMetric.getHostName())
                        .register(registry);

            }


    }

    @Override
    public MetricsConfig config() {
        return null;
    }

    @Override
    public void updateMetrics(Object... args) {

    }
}
