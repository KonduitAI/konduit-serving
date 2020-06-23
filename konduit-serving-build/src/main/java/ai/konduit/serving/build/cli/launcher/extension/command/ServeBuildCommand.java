/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.build.cli.launcher.extension.command;

import ai.konduit.serving.build.build.GradleBuild;
import ai.konduit.serving.build.config.Config;
import ai.konduit.serving.build.config.Serving;
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.deployments.ClassPathDeployment;
import ai.konduit.serving.cli.launcher.command.ServeCommand;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.guava.base.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Override
    public void run() {
        InferenceConfiguration inferenceConfiguration = getInferenceConfigurationFromFileOrString(configuration);
        if(inferenceConfiguration == null) {
            out.format("Invalid configuration defined by: %s", configuration);
            System.exit(1);
        } else {
            File gradleDir = new File(DirectoryFetcher.getBuildDir(), "gradle");
            File savePath = new File(DirectoryFetcher.getBuildDir(), String.format("%s-pipeline.json", getId()));
            File mfJar = new File(DirectoryFetcher.getBuildDir(), String.format("%s-manifest.jar", getId()));

            try {
                FileUtils.writeStringToFile(savePath,
                        ObjectMappers.toJson(inferenceConfiguration.getPipeline()),
                        StandardCharsets.UTF_8);

                Config c = new Config()
                        .pipelinePath(savePath.getAbsolutePath())
                        .target(Target.WINDOWS_X86_AVX2)
                        .serving(Serving.valueOf(inferenceConfiguration.getProtocol().name()))
                        .deployments(
                                new ClassPathDeployment().type(ClassPathDeployment.Type.JAR_MANIFEST).outputFile(mfJar.getAbsolutePath())
                        );

                GradleBuild.generateGradleBuildFiles(gradleDir, c);
                GradleBuild.runGradleBuild(gradleDir, c);

                if(Strings.isNullOrEmpty(this.classpath)) {
                    this.classpath = mfJar.getAbsolutePath();
                } else {
                    this.classpath += File.pathSeparator + mfJar.getAbsolutePath();
                }
            } catch (IOException e) {
                log.error("Unable to write build pipeline data to {}.", savePath.getAbsolutePath(), e);
                System.exit(1);
            } catch (Exception e) {
                log.error("Unable to build classpath manifest jar for the given pipeline and profile.", e);
                System.exit(1);
            }
        }

        super.run();
    }

    /**
     * Takes a file path to a valid JSON/YAML or a JSON String and parses it into {@link InferenceConfiguration}
     * @param jsonOrYamlFileOrString JSON/YAML file or a valid JSON String
     * @return {@link InferenceConfiguration} that was parsed
     */
    protected InferenceConfiguration getInferenceConfigurationFromFileOrString(String jsonOrYamlFileOrString) {
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
     * Parse the given configuration yaml/json string to {@link InferenceConfiguration}.
     *
     * @param configurationString given configuration string. Can be a file path or a JSON string
     * @return Read configuration to {@link InferenceConfiguration}. Returns null on failure.
     */
    private InferenceConfiguration readConfiguration(String configurationString) {
        try {
            return InferenceConfiguration.fromJson(configurationString);
        } catch (Exception jsonProcessingErrors) {
            try {
                return InferenceConfiguration.fromYaml(configurationString);
            } catch (Exception yamlProcessingErrors) {
                log.error("Given configuration: {} does not contain a valid JSON/YAML object", configurationString);
                log.error("\n\nErrors while processing as a json string:", jsonProcessingErrors);
                log.error("\n\nErrors while processing as a yaml string:", yamlProcessingErrors);
                return null;
            }
        }
    }
}
