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

import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.dependencies.NativeDependency;
import org.nd4j.common.base.Preconditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This is a PLACEHOLDER implementation... this native metadata will probably be redesigned (different collection
 * method, different storage method, etc)
 */
public class NativeDependencyRegistry {

    private static final Map<Dependency, NativeDependency> map = new HashMap<>();

    private static void put(Dependency d, Target... targets){
        map.put(d, new NativeDependency(d, new HashSet<>(Arrays.asList(targets))));
    }

    static {
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64"), Target.LINUX_X86, Target.LINUX_X86_AVX2, Target.LINUX_X86_AVX512);
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64-avx2"), Target.LINUX_X86_AVX2, Target.LINUX_X86_AVX512);
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64-avx512"), Target.LINUX_X86_AVX512);
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "windows-x86_64"), Target.WINDOWS_X86, Target.WINDOWS_X86_AVX2);
        put(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "windows-x86_64-avx2"), Target.WINDOWS_X86_AVX2);
    }




    public static boolean isNativeDependency(Dependency d){
        return map.containsKey(d);
    }

    public static NativeDependency getNativeDependency(Dependency d){
        Preconditions.checkState(isNativeDependency(d), "Not a native dependency");
        return map.get(d);
    }

}
