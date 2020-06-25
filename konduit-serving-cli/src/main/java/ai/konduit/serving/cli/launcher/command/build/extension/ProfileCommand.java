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

@Name("profile")
@Summary("Command to List, view, edit, create and delete konduit serving run profiles.")
@Description("A utility command to create, view, edit, list and delete konduit serving run profiles. Run profiles " +
        "configures the background run architecture such as CPU, GPU (CUDA). Konduit serving tries to identify the " +
        "best profiles during the first server launch but you can manage your own profile configurations with this " +
        "command. \n\n"+
        "Example usages:\n" +
        "--------------\n" +
        "- Creates a CUDA 10.2 profile with the name 'CUDA-10.2':\n" +
        "$ konduit profile create -t CUDA_10.2 -n CUDA-10.2\n\n" +
        "- Creates a simple profile for x86_avx2 architecture with name 'CPU-1':\n" +
        "$ konduit profile create -t x86_avx2 -n CPU-1\n\n" +
        "- Listing all the profiles:\n" +
        "$ konduit profile list\n\n" +
        "- Viewing a profile:\n" +
        "$ konduit profile view CPU-1\n\n" +
        "- Edit a profile with name 'CPU-1' from old type to 'x86':\n" +
        "$ konduit profile edit -n CPU-1 -t x86 \n\n" +
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
    private void setSubCommand(String subCommand) {
        this.subCommand = SubCommand.valueOf(subCommand);
    }

    @Option(shortName = "n", longName = "name", argName = "<profile_name>", required = true)
    @Description("Name of the profile which needs to be viewed, created, edited or deleted")
    private void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Option(shortName = "a", longName = "arch", argName = "cpu_architecture")
    @Description("Name of the cpu architecture. Accepted values are: x86, x86_64, x86_avx2, x86-avx2, x86_64-a, " +
            "x86_avx5, x86-avx5, x86_64-a, arm64, armhf, ppc64le]")
    private void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture;
    }

    @Option(shortName = "o", longName = "os", argName = "operating_system")
    @Description("Operating system the server needs to run at. Accepted values are: [windows, linux, mac, osx, macosx, android]")
    private void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    @Option(shortName = "cd", longName = "computeDevice", argName = "computeDevice")
    @Description("Compute device to use with the server. Accepted values are: [CPU, CUDA_10.0, CUDA_10.1, CUDA_10.2]")
    private void setComputeDevice(String computeDevice) {
        this.computeDevice = computeDevice;
    }

    @Option(shortName = "st", longName = "serverTypes", argName = "serverTypes", acceptMultipleValues = true)
    @Description("One or more space separated values, indicating the backend server type. Accepted values are: [HTTP, GRPC, MQTT]")
    private void setServerTypes(List<String> serverTypes) {
        this.serverTypes = serverTypes;
    }

    @Option(shortName = "ad", longName = "additionalDependencies", argName = "additionalDependencies", acceptMultipleValues = true)
    @Description("One or more space separated values (maven coordinates) indicating additional dependencies to be included with " +
            "the server launch. The pattern of additional dependencies should be either <group_id>:<artifact_id>:<version> or " +
            "<group_id>:<artifact_id>:<version>:<classifier>")
    private void setAdditionalDependencies(List<String> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    private enum SubCommand {
        CREATE, LIST, VIEW, EDIT, DELETE
    }

    @Override
    public void run() {
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
            saveProfile(profileName, fillProfileValues(getProfile(profileName)));
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
        if(serverTypes != null) {
            profile.serverTypes(serverTypes);
        }
        if(additionalDependencies != null) {
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
        if(profilesSavePath.exists()) {
            return firstTimeProfilesSetup();
        } else {
            try {
                return ObjectMappers.fromYaml(FileUtils.readFileToString(profilesSavePath, StandardCharsets.UTF_8), Map.class);
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
            } else {
                profiles.remove(profileName);
                saveProfiles(profiles);
                out.format("Deleted profile: %s successfully.%n", profileName);
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
        if(isProfileExists(profileName)) {
            out.format("Profile with name %s already exists", profileName);
        } else {
            Map<String, Profile> profiles = getAllProfiles();
            profiles.put(profileName, profile);
            saveProfiles(profiles);
        }
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
