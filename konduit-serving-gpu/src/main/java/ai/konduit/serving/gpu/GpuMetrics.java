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

package ai.konduit.serving.gpu;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;

import org.bytedeco.cuda.nvml.*;
import org.bytedeco.javacpp.BytePointer;

import java.nio.charset.StandardCharsets;

import static java.util.Collections.emptyList;
import static org.bytedeco.cuda.global.nvml.*;

/**
 * See https://docs.nvidia.com/deploy/nvml-api/group__nvmlDeviceQueries.html#group__nvmlDeviceQueries_1gf91efb38dadf591dd0de32ef7f0fd423
 * for information on the nvml queries used here.
 */
public class GpuMetrics implements MeterBinder {

    private final Iterable<Tag> tags;

    public GpuMetrics() {
        this(emptyList());
    }

    public GpuMetrics(Iterable<Tag> tags) {
        this.tags = tags;
    }


    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        checkReturn(nvmlInit_v2());

        int[] resultArr = new int[1];
        checkReturn(nvmlSystemGetCudaDriverVersion(resultArr));

        int deviceCount;
        checkReturn(nvmlDeviceGetCount_v2(resultArr));
        deviceCount = resultArr[0];

        Gauge.builder("system.gpu.count", () -> deviceCount)
                .tags(tags)
                .description("The number of gpus available in the system")
                .register(registry);

        System.out.format("Found %s GPU(s) in the system", deviceCount);

        for (int i = 0; i < deviceCount; i++) {
            nvmlDevice_st device = new nvmlDevice_st();
            nvmlDeviceGetHandleByIndex_v2(i, device);

            String gpuName;

            try {
                BytePointer namePointer = new BytePointer(NVML_DEVICE_NAME_BUFFER_SIZE);
                checkReturn(nvmlDeviceGetName(device, namePointer, NVML_DEVICE_NAME_BUFFER_SIZE));
                String gpuNameString = namePointer.getString(StandardCharsets.UTF_8);
                gpuName = gpuNameString.substring(0, gpuNameString.indexOf(Character.MIN_VALUE));
            } catch (Exception exception) {
                gpuName = "GPU " + i;
                System.out.format("Unable to resolve GPU name at index %s. Using name as: %s%n", i, gpuName);
                exception.printStackTrace();
            }

            String gpuIndexAndName = i + "." + gpuName.replace(" ", ".").toLowerCase();

            Gauge.builder(gpuIndexAndName + ".running.processes", () -> {
                        /*
                         * Note that the result array first entry has to be zero here.
                         * This will cause nvml to return the number of running graphics processes
                         * and nothing more.
                         *
                         * Note that we also don't call checkReturn here because it can return
                         * something that is not success.
                         * A success means zero processes are running.
                         * Anything else
                         */
                        resultArr[0] = 0;
                        nvmlProcessInfo_v1_t processInfoT = new nvmlProcessInfo_v1_t();

                        int result = nvmlDeviceGetGraphicsRunningProcesses(device, resultArr, processInfoT);
                        if (result != NVML_ERROR_INSUFFICIENT_SIZE && result != NVML_SUCCESS) {
                            throw new IllegalStateException("Number of running processes query failed " + nvmlErrorString(result));
                        }

                        return resultArr[0];
                    })
                    .tags(tags)
                    .description(String.format("Number of running processes on GPU %s", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".percent.memory.usage", () -> {
                        /*
                         * From docs
                         * ---------
                         * unsigned int memory
                         * Percent of time over the past sample period during which global (device) memory was being read or written.
                         */
                        nvmlUtilization_t nvmlUtilizationT = new nvmlUtilization_t();
                        if(checkReturn(nvmlDeviceGetUtilizationRates(device, nvmlUtilizationT)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return nvmlUtilizationT.memory();
                        }
                    })
                    .tags(tags)
                    .description(String.format("Percent gpu %s memory usage", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".percent.gpu.usage", () -> {
                        /*
                         * From docs
                         * ---------
                         * unsigned int gpu
                         * Percent of time over the past sample period during which one or more kernels was executing on the GPU.
                         */
                        nvmlUtilization_t nvmlUtilizationT = new nvmlUtilization_t();
                        if(checkReturn(nvmlDeviceGetUtilizationRates(device, nvmlUtilizationT)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return nvmlUtilizationT.gpu();
                        }
                    })
                    .tags(tags)
                    .description(String.format("Percent gpu %s process usage", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".memory.usage.megabytes", () -> {
                        nvmlMemory_t memory = new nvmlMemory_t();
                        if(checkReturn(nvmlDeviceGetMemoryInfo(device, memory)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return memory.used() / 1024 / 1024;
                        }
                    })
                    .tags(tags)
                    .description(String.format("Memory used on GPU %s", i))
                    .register(registry);

            nvmlMemory_t memory = new nvmlMemory_t();
            checkReturn(nvmlDeviceGetMemoryInfo(device, memory));
            Gauge.builder(gpuIndexAndName + ".total.gpu.memory.megabytes", () -> memory.total() / 1024 / 1024)
                    .description(String.format("Total memory on GPU %s", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".temp.celcius", () -> {
                        if(checkReturn(nvmlDeviceGetTemperature(device, NVML_TEMPERATURE_GPU, resultArr)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return resultArr[0];
                        }
                    })
                    .tags(tags)
                    .description(String.format("Temp on GPU %s", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".power.usage.milliwatts", () -> {
                        if(checkReturn(nvmlDeviceGetPowerUsage(device, resultArr)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return resultArr[0];
                        }
                    })
                    .tags(tags)
                    .description(String.format("Power utilization by GPU %s", i))
                    .register(registry);

            Gauge.builder(gpuIndexAndName + ".fan.speed.percent", () -> {
                        if(checkReturn(nvmlDeviceGetFanSpeed(device, resultArr)) == NVML_ERROR_NOT_SUPPORTED) {
                            return -1;
                        } else {
                            return resultArr[0];
                        }
                    })
                    .tags(tags)
                    .description(String.format("GPU %s fan speed", i))
                    .register(registry);
        }

        //nvmlShutdown();
    }

    private int checkReturn(int result) {
        if(result == NVML_ERROR_NOT_SUPPORTED) {
            return NVML_ERROR_NOT_SUPPORTED;
        } else {
            if (NVML_SUCCESS != result) {
                throw new IllegalStateException(String.format("Failed NVML call with error code: %s. Error details: %s", result, nvmlErrorString(result)));
            }

            return result;
        }
    }

}
