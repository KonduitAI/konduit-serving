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

package ai.konduit.serving.cli.launcher.command.build.extension.model;

import ai.konduit.serving.vertx.config.ServerProtocol;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.SystemUtils;
import org.nd4j.shade.jackson.annotation.JsonGetter;
import org.nd4j.shade.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
@ToString
public class Profile {

    private static final List<String> validComputeDevices = Arrays.asList("CPU", "CUDA_10.0", "CUDA_10.1", "CUDA_10.2");

    public enum CpuArchitecture {
        x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le;

        public static CpuArchitecture forName(String s) {
            switch (s.toLowerCase()) {
                case "x86":
                case "x86_64":
                    return CpuArchitecture.x86;
                case "x86_avx2":
                case "x86_64-avx2":
                    return CpuArchitecture.x86_avx2;
                case "x86_avx512":
                case "x86_64-avx512":
                    return CpuArchitecture.x86_avx512;
                case "arm64":
                    return CpuArchitecture.arm64;
                case "armhf":
                    return CpuArchitecture.armhf;
                case "ppc64le":
                    return CpuArchitecture.ppc64le;
                default:
                    return null;
            }
        }
    }

    public enum OperatingSystem {
        LINUX, WINDOWS, MAC;

        public static OperatingSystem forName(String s) {
            if ("MAC".equalsIgnoreCase(s) || "OSX".equalsIgnoreCase(s)) {
                return MAC;
            }

            return valueOf(s.toUpperCase());
        }
    }

    private String computeDevice;
    private CpuArchitecture cpuArchitecture;
    private OperatingSystem operatingSystem;
    private List<ServerProtocol> serverTypes;
    private List<String> additionalDependencies;

    public Profile() {
        this.computeDevice = "CPU";
        this.cpuArchitecture = CpuArchitecture.x86_avx2;
        this.operatingSystem = getCurrentOS();
        this.serverTypes = Arrays.asList(ServerProtocol.HTTP, ServerProtocol.GRPC);
        this.additionalDependencies = null;
    }

    public Profile(String computeDevice, String cpuArchitecture, String operatingSystem, List<String> serverTypes,
                        List<String> additionalDependencies) {
        computeDevice(computeDevice);
        this.cpuArchitecture = CpuArchitecture.forName(cpuArchitecture);
        this.operatingSystem = OperatingSystem.forName(operatingSystem);
        serverTypes(serverTypes);
        additionalDependencies(additionalDependencies);
    }

    @JsonSetter("computeDevice")
    public Profile computeDevice(String computeDevice) {
        if (validComputeDevices.contains(computeDevice)) {
            this.computeDevice = computeDevice;
        } else {
            throw new UnsupportedOperationException("Invalid, unknown, not supported or not yet implemented device type: " + computeDevice +
                    ". Valid values are: " + validComputeDevices);
        }

        return this;
    }

    @JsonSetter("operatingSystem")
    public Profile operatingSystem(String operatingSystem) {
        this.operatingSystem = OperatingSystem.forName(operatingSystem);

        return this;
    }

    @JsonSetter("cpuArchitecture")
    public Profile cpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = CpuArchitecture.forName(cpuArchitecture);

        return this;
    }

    @JsonSetter("serverTypes")
    public Profile serverTypes(List<String> serverTypes) {
        this.serverTypes = serverTypes != null ?
                serverTypes.stream().map(ServerProtocol::valueOf).collect(Collectors.toList()) :
                Arrays.asList(ServerProtocol.HTTP, ServerProtocol.GRPC);

        return this;
    }

    @JsonSetter("additionalDependencies")
    public Profile additionalDependencies(List<String> additionalDependencies) {
        if(additionalDependencies != null) {
            for (String additionalDependency : additionalDependencies) {
                String[] split = additionalDependency.split(":");
                if (split.length != 3 && split.length != 4) {
                    throw new IllegalStateException("Invalid additionalDependency setting: Dependencies must " +
                            "be specified in \"group_id:artifact_id:version\" or \"group_id:artifact_id:version:classifier\" format. Got " + additionalDependency);
                }
            }
        }

        this.additionalDependencies = additionalDependencies;

        return this;
    }

    @JsonGetter("computeDevice")
    public String computeDevice() {
        return this.computeDevice;
    }

    @JsonGetter("cpuArchitecture")
    public String cpuArchitecture() {
        return this.cpuArchitecture.name();
    }

    @JsonGetter("operatingSystem")
    public String operatingSystem() {
        return this.operatingSystem.name();
    }

    @JsonGetter("serverTypes")
    public List<String> serverTypes() {
        return this.serverTypes.stream().map(ServerProtocol::name).collect(Collectors.toList());
    }

    @JsonGetter("additionalDependencies")
    public List<String> additionalDependencies() {
        return this.additionalDependencies;
    }

    public static OperatingSystem getCurrentOS() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return OperatingSystem.WINDOWS;
        } else if (SystemUtils.IS_OS_LINUX) {
            return OperatingSystem.LINUX;
        } else if (SystemUtils.IS_OS_MAC) {
            return OperatingSystem.MAC;
        } else { // todo: find other operating systems if valid.
            throw new IllegalStateException("Invalid operating system specified. Should be one of: " + Arrays.asList(OperatingSystem.values()));
        }
    }
}
