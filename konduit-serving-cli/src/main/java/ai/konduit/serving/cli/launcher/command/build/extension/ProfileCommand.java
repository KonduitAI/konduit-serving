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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Name("profile")
@Summary("Command to List, view, edit, create and delete konduit serving run profiles.")
@Description("A utility command to create, view, edit, list and delete konduit serving run profiles. Run profiles " +
        "configures the background run architecture such as CPU, GPU (CUDA) along with additional dependencies, server " +
        "types, operating system etc. Konduit serving tries to identify the best profiles during the first server " +
        "launch but you can manage more of your own profile configurations with this command. \n\n"+
        "Example usages:\n" +
        "--------------\n" +
        "- Creates a CUDA 10.2 profile with the name 'CUDA-10.2':\n" +
        "$ konduit profile create CUDA-10.2 -t CUDA_10.2 \n\n" +
        "- Creates a simple profile for x86_avx2 architecture with name 'CPU-1':\n" +
        "$ konduit profile create CPU-1 -t x86_avx2\n\n" +
        "- Listing all the profiles:\n" +
        "$ konduit profile list\n\n" +
        "- Viewing a profile:\n" +
        "$ konduit profile view CPU-1\n\n" +
        "- Edit a profile with name 'CPU-1' from old type to 'x86':\n" +
        "$ konduit profile edit CPU-1 -t x86 \n\n" +
        "--------------")
@Slf4j
public class ProfileCommand extends DefaultCommand {

    private static final File profilesSavePath = new File(DirectoryFetcher.getProfilesDir(), "profiles.yaml");

    private SubCommand subCommand;
    private String profileName;
    private String cpuArchitecture;
    private String operatingSystem;
    private String computeDevice;
    private List<String> serverTypes;
    private List<String> additionalDependencies;

    @Argument(index = 0, argName = "subCommand")
    @Description("Sub command to be used with the profile command. Sub commands are: [create, list, view, edit, delete]")
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
    @Description("Name of the cpu architecture. Accepted values are: x86, x86_64, x86_avx2, x86-avx2, x86_64-a, " +
            "x86_avx5, x86-avx5, x86_64-a, arm64, armhf, ppc64le]")
    public void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture;
    }

    @Option(shortName = "o", longName = "os", argName = "operating_system")
    @Description("Operating system the server needs to run at. Accepted values are: [windows, linux, mac]. Defaults to the current OS value.")
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    @Option(shortName = "cd", longName = "computeDevice", argName = "computeDevice")
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

    @Option(shortName = "ad", longName = "additionalDependencies", argName = "additionalDependencies", acceptMultipleValues = true)
    @DefaultValue("ch.qos.logback:logback-classic:1.2.3")
    @Description("One or more space separated values (maven coordinates) indicating additional dependencies to be included with " +
            "the server launch. The pattern of additional dependencies should be either <group_id>:<artifact_id>:<version> or " +
            "<group_id>:<artifact_id>:<version>:<classifier>")
    public void setAdditionalDependencies(List<String> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    private enum SubCommand {
        CREATE, LIST, VIEW, EDIT, DELETE
    }

    @Override
    public void run() {
        if(profileName == null && !this.subCommand.equals(SubCommand.LIST)) {
            out.println("Please specify a profile name.");
            System.exit(1);
        }

        switch (this.subCommand) {
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
                out.format("Cannot edit default profiles with name 'CPU' or 'CUDA'.%n");
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
        if(serverTypes != null && !serverTypes.isEmpty()) {
            profile.serverTypes(serverTypes);
        }
        if(additionalDependencies != null && !additionalDependencies.isEmpty()) {
            profile.additionalDependencies(additionalDependencies);
        }

        return profile;
    }

    public static boolean isProfileExists(String profileName) {
        return getAllProfiles().containsKey(profileName);
    }

    public static Map<String, Profile> firstTimeProfilesSetup() {
        log.error("Performing first time profiles setup.");

        Map<String, Profile> profiles = new HashMap<>();

        // todo: detect pre-installed cuda version or cuda compatible gpus and cpu architecture.
        profiles.put("CPU", new Profile());
        profiles.put("CUDA", new Profile("CUDA_10.2", "x86_avx2", Profile.getCurrentOS().name(),
                Arrays.asList("HTTP", "GRPC"), Collections.singletonList("ch.qos.logback:logback-classic:1.2.3")));

        log.error("Created profiles: {}", ObjectMappers.toYaml(profiles));

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
                out.format("Cannot delete default profiles with name 'CPU' or 'CUDA'.%n");
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
}
