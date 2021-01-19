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

import ai.konduit.serving.build.cli.BuildCLI;
import ai.konduit.serving.cli.launcher.command.ServeCommand;
import ai.konduit.serving.cli.launcher.command.build.extension.model.Profile;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.pipeline.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.guava.base.Strings;
import org.nd4j.shade.jackson.databind.JsonNode;
import org.nd4j.shade.jackson.databind.node.JsonNodeFactory;
import org.nd4j.shade.jackson.databind.node.ObjectNode;
import org.nd4j.shade.jackson.databind.node.TextNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Name(value = "serve", priority = 1)
@Summary("Start a konduit server application")
@Description("Start a konduit server application. " +
        "The application is identified with an id that can be set using the `--serving-id` or `-id` option. " +
        "The application can be stopped with the `stop` command. " +
        "This command takes the `run` command parameters. To see the " +
        "run command parameters, execute `run --help`\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Starts a server in the foreground with an id of 'inf_server' using 'config.json' as configuration file:\n" +
        "$ konduit serve -id inf_server -c config.json\n\n" +
        "- Starts a server in the foreground with an id of 'inf_server' using 'config.json' as configuration file and CPU profile:\n" +
        "$ konduit serve -id inf_server -c config.json -p CPU\n\n" +
        "- Starts a server in the background with an id of 'inf_server' using 'config.yaml' as configuration file:\n" +
        "$ konduit serve -id inf_server -c config.yaml -b\n" +
        "--------------")
@Slf4j
public class ServeBuildCommand extends ServeCommand {

    private String profileName;
    private List<String> additionalDependencies;
    private boolean runWithoutManifestJar;

    @Option(shortName = "p", longName = "profileName", argName = "profile_name")
    @Description("Name of the profile to be used with the server launch.")
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Option(shortName = "ad", longName = "addDep", argName = "additional_dependencies")
    @Description("Additional dependencies to include with the launch")
    public void setAdditionalDependencies(List<String> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    @Option(shortName = "rwm", longName = "runWithoutManifest", argName = "run_without_manifest", flag = true)
    @Description("Do not create the manifest jar file before launching the server.")
    public void setRunWithoutManifestJar(boolean runWithoutManifestJar) {
        this.runWithoutManifestJar = runWithoutManifestJar;
    }

    @Override
    public void run() {
        File profileRootDir = new File(DirectoryFetcher.getBuildDir(), getId());
        if ((!profileRootDir.exists() || !profileRootDir.isDirectory()) && !profileRootDir.mkdir()) {
            log.error("Unable to create build directory for path: {}.", profileRootDir.getAbsolutePath());
            System.exit(1);
        }

        File savePath = new File(profileRootDir, "pipeline.json");
        File mfJar = new File(profileRootDir, "manifest.jar");

        try {
            JsonNode jsonConfiguration = getConfigurationFromFileOrString(configuration);
            if (jsonConfiguration == null) {
                out.format("Invalid JSON/YAML configuration or invalid configuration file path defined by: %n%s", configuration);
                System.exit(1);
            } else {
                if (false) { // todo: set this to !runWithoutManifestJar after the build command is working successfully with profiles
                    if (!(jsonConfiguration.has("host") || jsonConfiguration.has("port") ||
                            jsonConfiguration.has("pipeline"))) {
                        // Assume that it's a json for a konduit serving pipeline and not a complete inference configuration
                        jsonConfiguration = new ObjectNode(JsonNodeFactory.instance)
                                .put("host", this.host)
                                .put("port", this.port)
                                .set("pipeline", jsonConfiguration.deepCopy());
                    }

                    Object pipeline = jsonConfiguration.get("pipeline");
                    if (pipeline == null) {
                        out.format("Invalid JSON/YAML configuration or invalid configuration file path defined by: %n%s", configuration);
                        System.exit(1);
                    }

                    FileUtils.writeStringToFile(savePath, pipeline.toString(), StandardCharsets.UTF_8);

                    Profile profile = profileName != null ? ProfileCommand.getProfile(profileName) : ProfileCommand.getDefaultProfile();
                    if (profile == null) {
                        if (profileName == null) {
                            out.println("Couldn't find a default profile.");
                        } else {
                            out.format("Couldn't find a profile with the specified name: '%s'.%n", profileName);
                        }
                        System.exit(1);
                    }

                    // todo: add logic here for overriding python paths variable through profiles (kept for a separate PR).

                    List<String> args = new ArrayList<>();
                    args.add("-p");
                    args.add(savePath.getAbsolutePath());
                    args.add("-c");
                    args.add(String.format("classpath.outputFile=%s", mfJar.getAbsolutePath()));
                    args.add("classpath.type=JAR_MANIFEST");
                    args.add("-dt");
                    args.add("CLASSPATH");
                    args.add("-d");
                    args.add(profile.computeDevice());
                    args.add("-a");
                    args.add(profile.cpuArchitecture());
                    args.add("-o");
                    args.add(profile.operatingSystem());

                    if (profile.serverTypes() != null) {
                        for (String serverType : profile.serverTypes()) {
                            args.add("-s");
                            args.add(serverType);
                        }
                    }

                    List<String> additionalDeps = null;
                    if (profile.additionalDependencies() != null && !profile.additionalDependencies().isEmpty()) {
                        additionalDeps = new ArrayList<>(profile.additionalDependencies());
                    }
                    if (this.additionalDependencies != null && !this.additionalDependencies.isEmpty()) {
                        additionalDeps = new ArrayList<>();
                        additionalDeps.addAll(this.additionalDependencies);
                    }

                    if (additionalDeps != null) {
                        for (String ad : additionalDeps) {
                            args.add("-ad");
                            args.add(ad);
                        }
                    }


                    // Issue: https://github.com/KonduitAI/konduit-serving/issues/437
                    BuildCLI.main(args.toArray(new String[0]));     //TODO we could just call build tool directly instead of via CLI (more robust to refactoring, compile time args checking etc)

                    if (Strings.isNullOrEmpty(this.classpath)) {
                        this.classpath = mfJar.getAbsolutePath();
                    } else {
                        this.classpath += File.pathSeparator + mfJar.getAbsolutePath();
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unable to write build pipeline data to {}.", savePath.getAbsolutePath(), e);
            System.exit(1);
        } catch (Exception e) {
            log.error("Unable to build classpath manifest jar for the given pipeline and profile.", e);
            System.exit(1);
        }

        super.run();
    }

    /**
     * Takes a file path to a valid JSON/YAML or a JSON String and parses it into {@link JsonNode}
     *
     * @param jsonOrYamlFileOrString JSON/YAML file or a valid JSON String
     * @return {@link JsonNode} that was parsed
     */
    protected JsonNode getConfigurationFromFileOrString(String jsonOrYamlFileOrString) {
        if (jsonOrYamlFileOrString != null) {
            try (Scanner scanner = new Scanner(new File(jsonOrYamlFileOrString), "UTF-8").useDelimiter("\\A")) {
                return readConfiguration(scanner.next());
            } catch (FileNotFoundException e) {
                return readConfiguration(jsonOrYamlFileOrString);
            }
        } else {
            return null;
        }
    }

    /**
     * Parse the given configuration yaml/json string to {@link JsonNode}.
     *
     * @param configurationString given configuration string. Can be a JSON/YAML string
     * @return Read configuration to {@link JsonNode}. Returns null on failure.
     */
    private JsonNode readConfiguration(String configurationString) {
        try {
            return ObjectMappers.json().readTree(configurationString);
        } catch (Exception jsonProcessingErrors) {
            try {
                JsonNode jsonNode = ObjectMappers.yaml().readTree(configurationString);
                if (jsonNode instanceof TextNode) {
                    throw new FileNotFoundException("File does not exist at path: " + configurationString);
                } else {
                    return jsonNode;
                }
            } catch (Exception yamlProcessingErrors) {
                log.error("Given configuration: '{}' does not contain a valid JSON/YAML object", configurationString);
                log.error("\n\nErrors while processing as a json string:", jsonProcessingErrors);
                log.error("\n\nErrors while processing as a yaml string:", yamlProcessingErrors);
                return null;
            }
        }
    }
}
