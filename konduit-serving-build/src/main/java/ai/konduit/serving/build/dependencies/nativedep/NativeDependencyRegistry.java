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

package ai.konduit.serving.build.dependencies.nativedep;

import ai.konduit.serving.build.config.ComputeDevice;
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.config.devices.CUDADevice;
import ai.konduit.serving.build.dependencies.Dependency;
import org.apache.commons.lang3.arch.Processor;
import org.nd4j.common.base.Preconditions;

import java.util.*;

/**
 * This is a PLACEHOLDER implementation... this native metadata will probably be redesigned (different collection
 * method, different storage method, etc)
 */
public class NativeDependencyRegistry {
    public static final String LINUX = "linux";
    public static final String WINDOWS = "windows";
    public static final String MACOSX = "macosx";
    public static final String X86_64 = "x86_64";
    public static final String X86_64_AVX2 = "x86_64-avx2";
    public static final String X86_64_AVX512 = "x86_64-avx512";
    public static final String ARM64 = "arm64";
    public static final String ARMHF = "armhf";
    public static final String PPC64LE = "ppc64le";

    public static final String LINUX_X86_64 = LINUX + "-" + X86_64;
    public static final String LINUX_X86_AVX2 = LINUX + "-" + X86_64_AVX2;
    public static final String LINUX_X86_AVX512 = LINUX + "-" + X86_64_AVX512;
    public static final String WINDOWS_X86_64 = WINDOWS + "-" + X86_64;
    public static final String WINDOWS_X86_AVX2 = WINDOWS + "-" + X86_64_AVX2;
    public static final String MACOSX_X86_64 = MACOSX + "-" + X86_64;
    public static final String MACOSX_X86_AVX2 = MACOSX + "-" + X86_64_AVX2;

    private static final Map<Dependency, NativeDependency> map = new HashMap<>();

    private static void put(Dependency d, Target... targets){
        map.put(d, new NativeDependency(d, new HashSet<>(Arrays.asList(targets))));
    }

    static {
        //These are dependencies that can only run on a specific target
        //TODO - TF, ONNX, etc

        //ND4J native
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null), Target.LWM_X86);

        //CUDA
        put(new Dependency("org.nd4j", "nd4j-cuda-10.0", "1.0.0-beta7", null), Target.LINUX_CUDA_10_0, Target.WINDOWS_CUDA_10_0);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.1", "1.0.0-beta7", null), Target.LINUX_CUDA_10_1, Target.WINDOWS_CUDA_10_1);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.2", "1.0.0-beta7", null), Target.LINUX_CUDA_10_2, Target.WINDOWS_CUDA_10_2);
        //CUDA classifiers
        put(new Dependency("org.nd4j", "nd4j-cuda-10.0", "1.0.0-beta7", LINUX_X86_64), Target.LINUX_CUDA_10_0);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.1", "1.0.0-beta7", LINUX_X86_64), Target.LINUX_CUDA_10_1);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.2", "1.0.0-beta7", LINUX_X86_64), Target.LINUX_CUDA_10_2);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.0", "1.0.0-beta7", WINDOWS_X86_64), Target.WINDOWS_CUDA_10_0);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.1", "1.0.0-beta7", WINDOWS_X86_64), Target.WINDOWS_CUDA_10_1);
        put(new Dependency("org.nd4j", "nd4j-cuda-10.2", "1.0.0-beta7", WINDOWS_X86_64), Target.WINDOWS_CUDA_10_2);
    }

    public static boolean isNativeDependency(Dependency d){

        if(d.classifier() != null){
            String c = d.classifier();
            if(c.startsWith(LINUX) || c.startsWith(WINDOWS) || c.startsWith(MACOSX)){
                //JavaCPP and ND4J etc dependencies
                return true;
            }
        }

        return map.containsKey(d);
    }

    public static NativeDependency getNativeDependency(Dependency d){
        Preconditions.checkState(isNativeDependency(d), "Not a native dependency");

        if(d.classifier() != null){
            String c = d.classifier();
            if(c.startsWith(LINUX + "-") || c.startsWith(WINDOWS + "-") || c.startsWith(MACOSX + "-")){
                //JavaCPP and ND4J etc dependencies
                int idx = c.indexOf("-");
                String osStr = c.substring(0,idx);
                String archStr = c.substring(idx+1);
                Target.OS os = Target.OS.forName(osStr);
                Target.Arch arch = Target.Arch.forName(archStr);
                ComputeDevice device = deviceFor(d);

                Preconditions.checkState(arch != null, "Could not infer target architecture for %s", d);

                Target.Arch[] compatibleWith = arch.compatibleWith();
                Set<Target> supported = new HashSet<>();
                for(Target.Arch a : compatibleWith){
                    supported.add(new Target(os, a, device));
                }

                return new NativeDependency(d, supported);
            }
        }


        return map.get(d);
    }

    public static ComputeDevice deviceFor(Dependency d){
        //TODO this won't work for things like CUDA! And isn't robust to new versions... Need a more robust approach to this...
        String a = d.artifactId().toLowerCase();
        if(a.contains("cuda-10.0")){
            return new CUDADevice("10.0");
        } else if(a.contains("cuda-10.1")){
            return new CUDADevice("10.1");
        } else if(a.contains("cuda-10.2")){
            return new CUDADevice("10.2");
        }
        return null;
    }

}
