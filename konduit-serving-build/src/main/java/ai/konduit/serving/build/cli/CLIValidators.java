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

package ai.konduit.serving.build.cli;

import ai.konduit.serving.build.config.ComputeDevice;
import ai.konduit.serving.build.config.Deployment;
import ai.konduit.serving.build.config.target.Arch;
import ai.konduit.serving.build.config.target.OS;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.util.Arrays;
import java.util.List;

public class CLIValidators {

    private CLIValidators(){ }


    public static class OSValueValidator implements IValueValidator<List<String>> {
        private static final String LINUX = OS.LINUX.toString();
        private static final String WINDOWS = OS.WINDOWS.toString();
        private static final String MAC = "MAC";


        @Override
        public void validate(String name, List<String> value) throws ParameterException {
            for(String s : value){
                if(!LINUX.equalsIgnoreCase(s) && !WINDOWS.equalsIgnoreCase(s) && !MAC.equalsIgnoreCase(s)){
                    throw new ParameterException("Invalid operating system: got \"" + s + "\" but must be one or more of {" + LINUX + "," + WINDOWS + "," + MAC + "} (case insensitive)");
                }
            }
        }
    }

    public static class ArchValueValidator implements IValueValidator<String> {
        private static final String X86 = Arch.x86.toString();
        private static final String X86_AVX2 = Arch.x86_avx2.toString();
        private static final String X86_AVX512 = Arch.x86_avx512.toString();
        private static final String ARMHF = Arch.armhf.toString();
        private static final String ARM64 = Arch.arm64.toString();
        private static final String PPC64LE = Arch.ppc64le.toString();


        @Override
        public void validate(String name, String s) throws ParameterException {
            if(!X86.equalsIgnoreCase(s) && !X86_AVX2.equalsIgnoreCase(s) && !X86_AVX512.equalsIgnoreCase(s) &&
                    !ARMHF.equalsIgnoreCase(s) && !ARM64.equalsIgnoreCase(s) && !PPC64LE.equalsIgnoreCase(s)){
                throw new ParameterException("Invalid CPU architecture: Got \"" + s + "\" but must be one or more of {" + X86 + ", " + X86_AVX2 +
                        ", " + X86_AVX512 + ", " + ARMHF + ", " + ARM64 + ", " + PPC64LE + "} (case insensitive)");
            }
        }
    }

    public static class DeploymentTypeValueValidator implements IValueValidator<List<String>> {

        public static final List<String> VALUES = Arrays.asList(Deployment.JAR, Deployment.UBERJAR, Deployment.DOCKER, Deployment.EXE,
                Deployment.WAR, Deployment.RPM, Deployment.DEB, Deployment.TAR);

        @Override
        public void validate(String name, List<String> value) throws ParameterException {
            if(value == null || value.isEmpty()){
                throw new ParameterException("No deployment types were provided. Valid values are: " + VALUES + " (case insensitive)");
            }

            for(String s : value){
                boolean found = false;
                for(String s2 : VALUES){
                    if(s2.equalsIgnoreCase(s)){
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    throw new ParameterException("Invalid deployment type specified: \"" + s + "\" - valid values are: " + VALUES + " (case insensitive)");
                }
            }
        }
    }

    public static class ModuleValueValidator implements IValueValidator<List<String>> {

        @Override
        public void validate(String name, List<String> value) throws ParameterException {

        }
    }

    public static class ServerTypeValidator implements IValueValidator<List<String>> {
        private static final List<String> VALUES = Arrays.asList(BuildCLI.HTTP, BuildCLI.GRPC);

        @Override
        public void validate(String name, List<String> value) throws ParameterException {
            if(value == null || value.isEmpty()){
                throw new ParameterException("No server type were provided. Valid values are: " + VALUES + " (case insensitive)");
            }

            for(String s : value){
                boolean found = false;
                for(String s2 : VALUES){
                    if(s2.equalsIgnoreCase(s)){
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    throw new ParameterException("Invalid server type specified: \"" + s + "\" - valid values are: " + VALUES + " (case insensitive)");
                }
            }
        }
    }

    public static class DeviceValidator implements IValueValidator<String> {

        @Override
        public void validate(String name, String value) throws ParameterException {
            if(value == null || value.isEmpty())
                return; //EMPTY = CPU

            boolean ok = ComputeDevice.CPU.equalsIgnoreCase(value) ||
                    ComputeDevice.CUDA_100.equalsIgnoreCase(value) ||
                    ComputeDevice.CUDA_101.equalsIgnoreCase(value) ||
                    ComputeDevice.CUDA_102.equalsIgnoreCase(value);

            if(!ok){
                throw new ParameterException("Invalid device string: must be blank (not set = CPU), or have value " +
                        ComputeDevice.CPU + ", " + ComputeDevice.CUDA_100 + ", " + ComputeDevice.CUDA_101 + ", " +
                        ComputeDevice.CUDA_102);
            }
        }
    }

}
