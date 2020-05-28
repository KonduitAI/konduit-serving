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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.config.devices.CUDADevice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * The deployment target - OS, architecture, CUDA vs. CPU, etc
 */
@Data
@Accessors(fluent = true)
public class Target {
    public enum OS {LINUX, WINDOWS, OSX, ANDROID}
    public enum Arch {x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le }


    public static final Target LINUX_X86 = new Target(OS.LINUX, Arch.x86, null);
    public static final Target LINUX_X86_AVX2 = new Target(OS.LINUX, Arch.x86_avx2, null);
    public static final Target LINUX_X86_AVX512 = new Target(OS.LINUX, Arch.x86_avx512, null);

    public static final Target WINDOWS_X86 = new Target(OS.WINDOWS, Arch.x86, null);
    public static final Target WINDOWS_X86_AVX2 = new Target(OS.WINDOWS, Arch.x86_avx2, null);

    public static final Target LINUX_CUDA_10_0 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.0"));
    public static final Target LINUX_CUDA_10_1 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.1"));
    public static final Target LINUX_CUDA_10_2 = new Target(OS.LINUX, Arch.x86, new CUDADevice("10.2"));


    private OS os;
    private Arch arch;
    private ComputeDevice device;       //If null: CPU

    public Target(@JsonProperty("os") OS os, @JsonProperty("arch") Arch arch, @JsonProperty("device") ComputeDevice device){
        this.os = os;
        this.arch = arch;
        this.device = device;
    }

    @Override
    public String toString(){
        return "Target(" + os + "," + arch + (device == null ? "" : "," + device.toString()) + ")";
    }

}
