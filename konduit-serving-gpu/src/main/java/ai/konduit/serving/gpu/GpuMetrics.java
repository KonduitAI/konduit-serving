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
import org.bytedeco.cuda.nvml.nvmlDevice_st;
import org.bytedeco.cuda.nvml.nvmlMemory_t;
import org.bytedeco.cuda.nvml.nvmlProcessInfo_t;
import org.bytedeco.cuda.nvml.nvmlUtilization_t;

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
    public void bindTo(MeterRegistry registry) {
        checkReturn(nvmlInit_v2());

        int[] resultArr = new int[1];
        checkReturn(nvmlSystemGetCudaDriverVersion(resultArr));

        StringBuffer baseName = new StringBuffer();
        baseName.append("pipeline.cuda.device.");

        int deviceCount = 0;
        checkReturn(nvmlDeviceGetCount_v2(resultArr));
        deviceCount = resultArr[0];

        for(int i = 0; i < deviceCount; i++) {
            nvmlDevice_st device = new nvmlDevice_st();
            nvmlDeviceGetHandleByIndex_v2(i,device);

            StringBuffer deviceStats = new StringBuffer();
            deviceStats.append(baseName);
            //device index
            deviceStats.append(resultArr[0]);

            /**
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
            nvmlProcessInfo_t processInfoT = new nvmlProcessInfo_t();
            int result = nvmlDeviceGetGraphicsRunningProcesses(device,resultArr,processInfoT);
            if(result != NVML_ERROR_INSUFFICIENT_SIZE && result != NVML_SUCCESS) {
                throw new IllegalStateException("Number of running processes query failed "  + nvmlErrorString(result));
            }


            final int numRunningProcesses = resultArr[0];
            Gauge.builder(".processes.count",() -> numRunningProcesses)
                    .tags(tags)
                    .description("Number of running processes on the gpu")
                    .baseUnit(deviceStats.toString())
                    .register(registry);

            /**
             * From docs
             * unsigned int  gpu
             * Percent of time over the past sample period during which one or more kernels was executing on the GPU.
             * unsigned int  memory
             * Percent of time over the past sample period during which global (device) memory was being read or written.
             */
            nvmlUtilization_t utilization_t = new nvmlUtilization_t();
            checkReturn(nvmlDeviceGetUtilizationRates(device,utilization_t));
            final int percentMemoryUsed = utilization_t.memory();
            Gauge.builder(".percent.memory.used",() -> percentMemoryUsed)
                    .tags(tags)
                    .description("Percent memory used for device by index")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
            final int percentKernelExecuted = utilization_t.gpu();
            Gauge.builder(".percent.kernel.executed",() -> percentKernelExecuted)
                    .tags(tags)
                    .description("Percent time kernel was being executed on gpu")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
            nvmlMemory_t memory = new nvmlMemory_t();
            nvmlDeviceGetMemoryInfo(device,memory);
            final long deviceUsedMemoryInBytes = memory.used();
            Gauge.builder(".memory.used",() -> deviceUsedMemoryInBytes)
                    .tags(tags)
                    .description("Device memory used in bytes")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
            // free needs a Pointer and has void return type now
//            final long deviceMemoryFreeInBytes = memory.free();
//            Gauge.builder(".memory.free",() -> deviceMemoryFreeInBytes)
//                    .tags(tags)
//                    .description("Device memory free in bytes")
//                    .baseUnit(deviceStats.toString())
//                    .register(registry);
            checkReturn(nvmlDeviceGetTemperature(device,NVML_TEMPERATURE_GPU,resultArr));
            final int tempC = resultArr[0];
            Gauge.builder(".temp.celsius",() -> tempC)
                    .tags(tags)
                    .description("Temperature of gpu in celsius")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
            final int tempF = tempC * 9 / 5 + 32;
            Gauge.builder(".temp.farenheit",() -> tempF)
                    .tags(tags)
                    .description("Temperature of gpu in farenheit")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
            checkReturn(nvmlDeviceGetPowerUsage(device,resultArr));
            final int powerUsedMilliWatts = resultArr[0];
            Gauge.builder(".power.used.milliwatts",() -> powerUsedMilliWatts)
                    .tags(tags)
                    .description("Power used by gpu in milliwatts")
                    .baseUnit(deviceStats.toString())
                    .register(registry);
        }

        nvmlShutdown();
    }

    private void checkReturn(int result) {
        if (NVML_SUCCESS != result) {
            throw new IllegalStateException("Failed nvmml call" + nvmlErrorString(result));
        }

    }

}
