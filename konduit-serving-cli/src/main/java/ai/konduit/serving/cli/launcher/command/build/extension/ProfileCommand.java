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

package ai.konduit.serving.cli.launcher.command.build.extension;

import ai.konduit.serving.cli.launcher.command.build.extension.model.Profile;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nd4j.common.primitives.Pair;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("profile")
@Summary("Command to List, view, edit, create and delete konduit serving run profiles.")
@Description("A utility command to create, view, edit, list and delete konduit serving run profiles. Run profiles " +
        "configures the background run architecture such as CPU, GPU (CUDA) along with additional dependencies, server " +
        "types, operating system etc. Konduit serving tries to identify the best profiles during the first server " +
        "launch but you can manage more of your own profile configurations with this command. \n\n"+
        "Example usages:\n" +
        "--------------\n" +
        "- Creates a CUDA 10.2 profile with the name 'CUDA-10.2':\n" +
        "$ konduit profile create CUDA-10.2 -d CUDA_10.2 \n\n" +
        "- Creates a simple profile for x86_avx2 architecture with name 'CPU-1':\n" +
        "$ konduit profile create CPU-1 -a x86_avx2\n\n" +
        "- Listing all the profiles:\n" +
        "$ konduit profile list\n\n" +
        "- Viewing a profile:\n" +
        "$ konduit profile view CPU-1\n\n" +
        "- Edit a profile with name 'CPU-1' from old architecture to 'x86':\n" +
        "$ konduit profile edit CPU-1 -a x86 \n" +
        "--------------")
@Slf4j
public class ProfileCommand extends DefaultCommand {

    private static final File profilesSavePath = new File(DirectoryFetcher.getProfilesDir(), "profiles.yaml");
    private static final File defaultProfileNamePath = new File(DirectoryFetcher.getProfilesDir(), "default");

    private SubCommand subCommand;
    private String profileName;
    private String cpuArchitecture;
    private String operatingSystem;
    private String computeDevice;
    private List<String> serverTypes;
    private List<String> additionalDependencies;

    @Argument(index = 0, argName = "subCommand")
    @Description("Sub command to be used with the profile command. Sub commands are: [default, create, list, view, edit, delete]")
    public void setSubCommand(String subCommand) {
        this.subCommand = SubCommand.valueOf(subCommand.toUpperCase());
    }

    @Argument(index = 1, argName = "profile_name", required = false)
    @Description("Name of the profile to create, view, edit or delete.")
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Option(shortName = "a", longName = "arch", argName = "cpu_architecture")
    @DefaultValue("x86_avx2")
    @Description("Name of the cpu architecture. Accepted values are: [x86, x86_64, x86_avx2, x86-avx2, x86_64-a, " +
            "x86_avx5, x86-avx5, x86_64-a, arm64, armhf, ppc64le]")
    public void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture;
    }

    @Option(shortName = "o", longName = "os", argName = "operating_system")
    @Description("Operating system the server needs to run at. Accepted values are: [windows, linux, mac]. Defaults to the current OS value.")
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    @Option(shortName = "d", longName = "device", argName = "device")
    @DefaultValue("CPU")
    @Description("Compute device to use with the server. Accepted values are: [CPU, CUDA_10.0, CUDA_10.1, CUDA_10.2]")
    public void setComputeDevice(String computeDevice) {
        this.computeDevice = computeDevice;
    }

    @Option(shortName = "st", longName = "serverTypes", argName = "serverTypes", acceptMultipleValues = true)
    @DefaultValue("HTTP GRPC")
    @Description("One or more space separated values, indicating the backend server type. Accepted values are: [HTTP, GRPC, MQTT]")
    public void setServerTypes(List<String> serverTypes) {
        this.serverTypes = serverTypes;
    }

    @Option(shortName = "ad", longName = "addDep", argName = "additionalDependencies", acceptMultipleValues = true)
    @DefaultValue("ch.qos.logback:logback-classic:1.2.3")
    @Description("One or more space separated values (maven coordinates) indicating additional dependencies to be included with " +
            "the server launch. The pattern of additional dependencies should be either <group_id>:<artifact_id>:<version> or " +
            "<group_id>:<artifact_id>:<version>:<classifier>")
    public void setAdditionalDependencies(List<String> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    private enum SubCommand {
        DEFAULT, CREATE, LIST, VIEW, EDIT, DELETE
    }

    @Override
    public void run() {
        if(profileName == null && !this.subCommand.equals(SubCommand.LIST)) {
            out.println("Please specify a profile name.");
            System.exit(1);
        }

        switch (this.subCommand) {
            case DEFAULT:
                setDefaultProfile(profileName);
                break;
            case LIST:
                listProfiles();
                break;
            case EDIT:
                editProfile();
                break;
            case DELETE:
                deleteProfile(profileName);
                break;
            case VIEW:
                viewProfile(profileName);
                break;
            case CREATE:
                createProfile();
                break;
        }
    }

    public static void setDefaultProfile(String profileName) {
        if(!isProfileExists(profileName)) {
            log.error("No profile with name {} exists.", profileName);
        } else {
            try {
                FileUtils.writeStringToFile(defaultProfileNamePath, profileName, StandardCharsets.UTF_8);
                log.error("Successfully set '{}' profile as default.", profileName);
            } catch (IOException e) {
                log.error("Unable to set default profile", e);
            }
        }
    }

    public static Profile getDefaultProfile() {
        try {
            String profileName = FileUtils.readFileToString(defaultProfileNamePath, StandardCharsets.UTF_8);
            return getProfile(profileName);
        } catch (IOException e) {
            log.error("Unable to get default profile", e);
            return null;
        }
    }

    private void createProfile() {
        if(isProfileExists(profileName)) {
            out.format("Profile with name %s already exists.%n", profileName);
        } else {
            saveProfile(profileName, fillProfileValues(new Profile()));
        }
    }

    private void editProfile() {
        if(isProfileExists(profileName)) {
            if(profileName.equals("CPU") || profileName.equals("CUDA")) {
                out.format("Cannot edit pre-set profiles with name 'CPU' or 'CUDA'.%n");
                System.exit(1);
            } else {
                saveProfile(profileName, fillProfileValues(getProfile(profileName)));
            }
        } else {
            out.format("No profile found with the name of %s%n", profileName);
        }
    }

    private Profile fillProfileValues(@NonNull Profile profile) {
        if(cpuArchitecture != null) {
            profile.cpuArchitecture(cpuArchitecture);
        }

        if(operatingSystem != null) {
            profile.operatingSystem(operatingSystem);
        }
        if(computeDevice != null) {
            profile.computeDevice(computeDevice);
        }

        if(StringUtils.containsIgnoreCase(profile.computeDevice(), "cuda")) {
            profile.cpuArchitecture("x86");
        }

        if(serverTypes != null && !serverTypes.isEmpty()) {
            profile.serverTypes(serverTypes);
        }
        if(additionalDependencies != null && !additionalDependencies.isEmpty()) {
            profile.additionalDependencies(additionalDependencies);
        }

        if(StringUtils.containsIgnoreCase(profile.computeDevice(), "cuda")) {
            String cudaVersion = profile.computeDevice().split("_")[1].trim();
            String cudaRedistPackage = null;
            Pair<String, String> cudaInstall = findCudaInstall();
            if(cudaInstall == null) {
                switch (cudaVersion) {
                    case "10.0":
                        out.format("No CUDA install found and no available redist package for cuda version: '%s' found. " +
                                "Make sure to install CUDA from: %s before starting a konduit server.%n", cudaVersion,
                                "https://developer.nvidia.com/cuda-10.0-download-archive");
                        break;
                    case "10.1":
                        cudaRedistPackage = "org.bytedeco:cuda-platform-redist:10.1-7.6-1.5.2";
                        break;
                    case "10.2":
                        cudaRedistPackage = "org.bytedeco:cuda-platform-redist:10.2-7.6-1.5.3";
                        break;
                    default:
                        throw new IllegalStateException("Unsupported cuda version: " + cudaVersion);
                }

                if(cudaRedistPackage != null && !profile.additionalDependencies().contains(cudaRedistPackage)) {
                    out.format("No cuda install found. Adding cuda redist package: %s as an additional dependency. " +
                            "This will be downloaded and setup automatically on runtime konduit server start build.%n",
                            cudaRedistPackage);
                    List<String> additionalDependencies = new ArrayList<>(profile.additionalDependencies());
                    additionalDependencies.add(cudaRedistPackage);
                    profile.additionalDependencies(additionalDependencies);
                }
            } else {
                if(!cudaVersion.equals(cudaInstall.getKey())) {
                    out.format("Installed cuda version %s is not the same as the profile cuda version %s.%n", cudaInstall.getKey(), cudaVersion);

                    switch (cudaVersion) {
                        case "10.0":
                            out.format("No available redist package for cuda version: '%s' found. " +
                                            "Make sure to install CUDA from: %s before starting a konduit server.%n", cudaVersion,
                                    "https://developer.nvidia.com/cuda-10.0-download-archive");
                            break;
                        case "10.1":
                            cudaRedistPackage = "org.bytedeco:cuda-platform-redist:10.1-7.6-1.5.2";
                            break;
                        case "10.2":
                            cudaRedistPackage = "org.bytedeco:cuda-platform-redist:10.2-7.6-1.5.3";
                            break;
                        default:
                            throw new IllegalStateException("Unsupported cuda version: " + cudaVersion);
                    }
                    if(cudaRedistPackage != null && !profile.additionalDependencies().contains(cudaRedistPackage)) {
                        out.format("Adding cuda redist package: %s as an additional dependency. This will be " +
                                        "downloaded and setup automatically on runtime konduit server start build.%n",
                                cudaRedistPackage);
                        List<String> additionalDependencies = new ArrayList<>(profile.additionalDependencies());
                        additionalDependencies.add(cudaRedistPackage);
                        profile.additionalDependencies(additionalDependencies);
                    }
                }
            }
        }

        return profile;
    }

    public static boolean isProfileExists(String profileName) {
        return getAllProfiles().containsKey(profileName);
    }

    public static Map<String, Profile> firstTimeProfilesSetup() {
        log.error("Performing first time profiles setup.");

        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor.ProcessorIdentifier processorIdentifier = systemInfo.getHardware().getProcessor().getProcessorIdentifier();
        String cpuArch = processorIdentifier.getMicroarchitecture();

        Map<String, Profile> profiles = new HashMap<>();
        Profile cpuProfile = new Profile();
        if(cpuArch != null) {
            if (StringUtils.containsIgnoreCase(cpuArch, "arm")) {
                if(processorIdentifier.isCpu64bit()) {
                    cpuProfile.cpuArchitecture("arm64");
                } else {
                    cpuProfile.cpuArchitecture("armhf");
                }
            }
        }

        String cpuProfileName = "CPU";
        profiles.put(cpuProfileName, cpuProfile);

        log.error("Looking for CUDA compatible devices in the current system...");
        List<GraphicsCard> nvidiaGraphicsCard = new ArrayList<>();
        for(GraphicsCard graphicsCard : systemInfo.getHardware().getGraphicsCards()) {
            String vendor = graphicsCard.getVendor();
            if (vendor != null && StringUtils.containsIgnoreCase(vendor, "nvidia")) {
                nvidiaGraphicsCard.add(graphicsCard);
            }
        }

        if(!nvidiaGraphicsCard.isEmpty() && !cpuProfile.operatingSystem().equalsIgnoreCase("mac")) {
            log.error("Found the following cuda compatible devices in the local system: {}", nvidiaGraphicsCard);

            String defaultCudaVersion =  "CUDA_10.1";
            Profile cudaProfile = new Profile(defaultCudaVersion,
                    "x86", cpuProfile.operatingSystem(), Arrays.asList("HTTP", "GRPC"),
                    Collections.singletonList("ch.qos.logback:logback-classic:1.2.3"));

            Pair<String, String> cudaInstall = findCudaInstall();
            if(cudaInstall != null) {
                log.error("Found CUDA install -- Version: {} | Path: {}", cudaInstall.getKey(), cudaInstall.getValue() != null ? cudaInstall.getValue() : "(Unable to identify)");

                cudaProfile.computeDevice(String.format("CUDA_%s", cudaInstall.getKey().trim()));
            } else {
                log.error("Unable to find a valid cuda install in the local system. The server will try to " +
                        "automatically download the CUDA redist 10.1 package on runtime build");

                List<String> additionalDependencies = new ArrayList<>(cudaProfile.additionalDependencies());
                additionalDependencies.add("org.bytedeco:cuda-platform-redist:10.1-7.6-1.5.2");
                cudaProfile.additionalDependencies(additionalDependencies);
            }

            String cudaProfileName = "CUDA";
            profiles.put(cudaProfileName, cudaProfile);
            setDefaultProfile(cudaProfileName);
        } else {
            log.error("No cuda compatible devices found in the current system.");
            setDefaultProfile(cpuProfileName);
        }

        log.error("Created profiles: \n{}", ObjectMappers.toYaml(profiles));

        saveProfiles(profiles);
        return profiles;
    }

    public static Map<String, Profile> getAllProfiles() {
        if(!profilesSavePath.exists()) {
            return firstTimeProfilesSetup();
        } else {
            try {
                Map<String, Profile> profiles = new HashMap<>();
                Map profilesMap = ObjectMappers.fromYaml(FileUtils.readFileToString(profilesSavePath, StandardCharsets.UTF_8), Map.class);
                for(Object key : profilesMap.keySet()) {
                    Profile profile = ObjectMappers.json().convertValue(profilesMap.get(key), Profile.class);
                    profiles.put((String) key, profile);
                }
                return profiles;
            } catch (IOException e) {
                log.error("Unable to read profiles data from {}.", profilesSavePath.getAbsolutePath(), e);
                System.exit(1);
                return null;
            }
        }
    }

    public void deleteProfile(String profileName) {
        Map<String, Profile> profiles = getAllProfiles();
        if(profiles.containsKey(profileName)) {
            if(profileName.equals("CPU") || profileName.equals("CUDA")) {
                out.format("Cannot delete pre-set profiles with name 'CPU' or 'CUDA'.%n");
                System.exit(1);
            } else {
                profiles.remove(profileName);
                saveProfiles(profiles);
                out.format("Deleted %s profile, successfully.%n", profileName);
            }
        } else {
            out.format("Profile with name: %s doesn't exist.%n", profileName);
        }
    }

    public static Profile getProfile(String profileName) {
        if(isProfileExists(profileName)) {
            return getAllProfiles().get(profileName);
        } else {
            log.error("Profile with name: {} doesn't exist.", profileName);
            return null;
        }
    }

    private void viewProfile(String profileName) {
        if(isProfileExists(profileName)) {
            out.println(ObjectMappers.toYaml(getAllProfiles().get(profileName)));
        } else {
            out.format("Profile with name: %s doesn't exist.%n", profileName);
        }
    }

    private void listProfiles() {
        out.println(ObjectMappers.toYaml(getAllProfiles()));
    }

    private void saveProfile(String profileName, Profile profile) {
        Map<String, Profile> profiles = getAllProfiles();
        profiles.put(profileName, profile);
        saveProfiles(profiles);
        out.format("Profile %s saved with details:%n%s%n", profileName, ObjectMappers.toYaml(profile));
    }

    private static void saveProfiles(Map<String, Profile> profiles) {
        try {
            FileUtils.writeStringToFile(profilesSavePath,
                    ObjectMappers.toYaml(profiles),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Unable to save profiles to {}", profilesSavePath.getAbsolutePath(), e);
            System.exit(1);
        }
    }

    private static Pair<String, String> findCudaInstall() {
        Pair<String, String> cudaInstallFromNvcc = findCudaInstallFromNvcc();
        if(cudaInstallFromNvcc != null) {
            return cudaInstallFromNvcc;
        } else {
            return findCudaInstallFromNvidiaSmi();
        }
    }

    private static Pair<String, String> findCudaInstallFromNvcc() {
        String mainCommand = "nvcc";

        try {
            String cudaVersion = findCudaVersion(Arrays.asList(mainCommand, "--version"), Pattern.compile("release (.*),"));
            if(cudaVersion == null) {
                return null;
            } else {
                return new Pair<>(cudaVersion, findCudaInstallPath(mainCommand, cudaVersion));
            }
        } catch (Exception exception) {
            log.error("Couldn't find cuda version from {} command", mainCommand, exception);
            System.exit(1);
            return null;
        }
    }

    private static Pair<String, String> findCudaInstallFromNvidiaSmi() {
        String mainCommand = "nvidia-smi";

        try {
            String cudaVersion = findCudaVersion(Collections.singletonList(mainCommand), Pattern.compile("CUDA Version:\b+(.*)\b+"));
            if(cudaVersion == null) {
                return null;
            } else {
                return new Pair<>(cudaVersion, findCudaInstallPath(mainCommand, cudaVersion));
            }
        } catch (Exception exception) {
            log.error("Couldn't find cuda version from {} command", mainCommand, exception);
            System.exit(1);
            return null;
        }
    }

    private static String findCudaVersion(List<String> command,  Pattern pattern) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ProcessBuilder(command).start().getInputStream()))) {
            String line = bufferedReader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    return matcher.group(1).trim();
                }
                line = bufferedReader.readLine();
            }
        }

        return null;
    }

    private static String findCudaInstallPath(String mainCommandName, String cudaVersion) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ProcessBuilder(Arrays.asList("where", mainCommandName)).start().getInputStream()))) {
            String line = bufferedReader.readLine();
            while (line != null) {
                if(line.contains(cudaVersion)) {
                    File parentFile = new File(line.trim()).getParentFile().getParentFile(); // to go back from <cuda_install_path>/bin/nvcc to <cuda_install_path>
                    if(parentFile.exists()) {
                        return parentFile.getAbsolutePath();
                    }
                    break;
                }
                line = bufferedReader.readLine();
            }
        }

        return null;
    }
}
