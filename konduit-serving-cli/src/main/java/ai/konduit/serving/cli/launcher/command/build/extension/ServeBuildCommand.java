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

import ai.konduit.serving.cli.launcher.command.ServeCommand;
import ai.konduit.serving.cli.launcher.command.build.extension.model.Profile;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.guava.base.Strings;
import org.nd4j.shade.jackson.databind.JsonNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
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

    private String profileName = "CPU";

    @Option(shortName = "p", longName = "profileName", argName = "profile_name")
    @Description("Name of the profile to be used with the server launch.")
    @DefaultValue("CPU")
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public void run() {
        File savePath = new File(DirectoryFetcher.getBuildDir(), String.format("%s-pipeline.json", getId()));
        File mfJar = new File(DirectoryFetcher.getBuildDir(), String.format("%s-manifest.jar", getId()));

        try {
            JsonNode jsonConfiguration = getConfigurationFromFileOrString(configuration);
            if (jsonConfiguration == null) {
                out.format("Invalid configuration defined by: %s", configuration);
                System.exit(1);
            } else {
                FileUtils.writeStringToFile(savePath, jsonConfiguration.get("pipeline").toString(), StandardCharsets.UTF_8);

                Class<?> buildCliClass = Class.forName("ai.konduit.serving.build.cli.BuildCLI");
                Object buildCli = buildCliClass.getConstructor().newInstance();
                Method buildCliMainMethod = buildCliClass.getMethod("exec", String[].class);
                buildCliMainMethod.setAccessible(true);

                Profile profile = ProfileCommand.getProfile(profileName);

                List<String> args = new ArrayList<>();
                args.add("-p"); args.add(savePath.getAbsolutePath());
                args.add("-c"); args.add(String.format("classpath.outputFile=%s", mfJar.getAbsolutePath())); args.add("classpath.type=JAR_MANIFEST");
                args.add("-dt"); args.add("CLASSPATH");
                args.add("-d"); args.add(profile.computeDevice());
                args.add("-a"); args.add(profile.cpuArchitecture());
                args.add("-o"); args.add(profile.operatingSystem());

                if(profile.serverTypes() != null) {
                    for(String serverType : profile.serverTypes()) {
                        args.add("-s"); args.add(serverType);
                    }
                }

                if(profile.additionalDependencies() != null) {
                    for(String additionalDependency : profile.additionalDependencies()) {
                        args.add("-ad"); args.add(additionalDependency);
                    }
                }

                buildCliMainMethod.invoke(buildCli, (Object) args.toArray(new String[args.size()]));

                if (Strings.isNullOrEmpty(this.classpath)) {
                    this.classpath = mfJar.getAbsolutePath();
                } else {
                    this.classpath += File.pathSeparator + mfJar.getAbsolutePath();
                }
            }
        } catch (IOException e) {
            log.error("Unable to write build pipeline data to {}.", savePath.getAbsolutePath(), e);
            System.exit(1);
        } catch (ClassNotFoundException e) {
            out.println("Unable to find classes for building manifest jar. Continuing without runtime build...");
        } catch (Exception e) {
            log.error("Unable to build classpath manifest jar for the given pipeline and profile.", e);
            System.exit(1);
        }

        super.run();
    }

    /**
     * Takes a file path to a valid JSON/YAML or a JSON String and parses it into {@link JsonNode}
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
     * @param configurationString given configuration string. Can be a file path or a JSON string
     * @return Read configuration to {@link JsonNode}. Returns null on failure.
     */
    private JsonNode readConfiguration(String configurationString) {
        try {
            return ObjectMappers.json().readTree(configurationString);
        } catch (Exception jsonProcessingErrors) {
            try {
                return ObjectMappers.yaml().readTree(configurationString);
            } catch (Exception yamlProcessingErrors) {
                log.error("Given configuration: {} does not contain a valid JSON/YAML object", configurationString);
                log.error("\n\nErrors while processing as a json string:", jsonProcessingErrors);
                log.error("\n\nErrors while processing as a yaml string:", yamlProcessingErrors);
                return null;
            }
        }
    }
}
