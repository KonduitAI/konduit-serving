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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.config.devices.CUDADevice;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * The Target class represents a deployment device/target - as defined in terms of an operatin system, CPU architecture,
 * and device (such as CPU vs. CUDA, etc). If no device is specified, it is assumed that CPU execution will be used.
 */
@Data
@Accessors(fluent = true)
public class Target {

    public static final Target LINUX_X86 = new Target(OS.LINUX, Arch.x86, null);
    public static final Target LINUX_X86_AVX2 = new Target(OS.LINUX, Arch.x86_avx2, null);
    public static final Target LINUX_X86_AVX512 = new Target(OS.LINUX, Arch.x86_avx512, null);

    public static final Target WINDOWS_X86 = new Target(OS.WINDOWS, Arch.x86, null);
    public static final Target WINDOWS_X86_AVX2 = new Target(OS.WINDOWS, Arch.x86_avx2, null);

    public static final Target MACOSX_X86 = new Target(OS.MACOSX, Arch.x86, null);
    public static final Target MACOSX_X86_AVX2 = new Target(OS.MACOSX, Arch.x86_avx2, null);

    public static final Target LINUX_CUDA_10_0 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.0"));
    public static final Target LINUX_CUDA_10_1 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.1"));
    public static final Target LINUX_CUDA_10_2 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.2"));
    public static final Target WINDOWS_CUDA_10_0 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.0"));
    public static final Target WINDOWS_CUDA_10_1 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.1"));
    public static final Target WINDOWS_CUDA_10_2 = new Target(OS.WINDOWS, Arch.x86, new CUDADevice("10.2"));

    /** Linux, Windows and Mac x86, x86 avx2 and avx512 */
    public static final Target[] LWM_X86 = new Target[]{LINUX_X86, LINUX_X86_AVX2, LINUX_X86_AVX512, WINDOWS_X86, WINDOWS_X86_AVX2,
            MACOSX_X86, MACOSX_X86_AVX2};

    private final OS os;
    private final Arch arch;
    private final ComputeDevice device;       //If null: CPU

    public Target(@JsonProperty("os") OS os, @JsonProperty("arch") Arch arch, @JsonProperty("device") ComputeDevice device){
        this.os = os;
        this.arch = arch;
        this.device = device;
    }

    @Override
    public String toString(){
        return "Target(" + os + "," + arch + (device == null ? "" : "," + device.toString()) + ")";
    }

    public String toJavacppPlatform(){
        //https://github.com/bytedeco/javacpp/tree/master/src/main/resources/org/bytedeco/javacpp/properties
        return os.name() + "-" + arch.name();
    }
}
