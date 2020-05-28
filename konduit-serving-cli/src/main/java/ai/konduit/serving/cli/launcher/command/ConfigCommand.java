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

package ai.konduit.serving.cli.launcher.command;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Name("config")
@Summary("A helper command for creating JSON for inference configuration")
@Description("This command is a utility to create json configurations that can be consumed to start konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Prints 'TENSORFLOW' config in pretty format:\n" +
        "$ konduit config -t TENSORFLOW\n\n" +
        "- Prints 'IMAGE + DL4J' config in minified format:\n" +
        "$ konduit config -t IMAGE,DL4J -m\n\n" +
        "- Saves 'IMAGE + DL4J' config in a 'config.json' file:\n" +
        "$ konduit config -t IMAGE,DL4J -o config.json\n" +
        "--------------")
@Slf4j
public class ConfigCommand extends DefaultCommand {

    public static final String DEFAULT = "default";

    private enum ConfigType {
        LOGGING
    }

    private String types;
    private boolean minified;
    private boolean yaml;
    private File outputFile;

    @Option(longName = "types", shortName = "t", argName = "config-types", required = true)
    @Description("A comma-separated list of pipeline steps you want to create boilerplate configuration for. " +
            "Allowed values are: [logging]")
    public void setTypes(String types) {
        this.types = types;
    }

    @Option(longName = "minified", shortName = "m", flag = true)
    @Description("If set, the output json will be printed in a single line, without indentations.")
    public void setMinified(boolean minified) {
        this.minified = minified;
    }

    @Option(longName = "yaml", shortName = "y", flag = true)
    @Description("Set if you want the output to be a yaml configuration.")
    public void setYaml(boolean yaml) { this.yaml = yaml; }

    @Option(longName = "output", shortName = "o", argName = "output-file")
    @Description("Optional: If set, the generated json will be saved here. Otherwise, it's printed on the console.")
    public void setOutputFile(String output) {
        outputFile = new File(output);
        if(outputFile.exists()) {
            if(!outputFile.isFile()) {
                log.error("'{}' is not a valid file location", outputFile);
            }
        } else {
            try {
                if(!outputFile.createNewFile()) {
                    log.error("'{}' is not a valid file location", outputFile);
                }
            } catch (Exception exception) {
                log.error("Error while creating file: '{}'", outputFile, exception);
            }
        }
    }

    @Override
    public void run() {
        SequencePipeline.Builder builder = SequencePipeline.builder();

        for (String type : types.split(",")) {
            try {
                switch (ConfigType.valueOf(type.toUpperCase().trim())) {
                    case LOGGING:
                        builder.add(logging());
                        break;
                    default:
                        log.error("Invalid config type '{}'. Allowed values are {}", type, Arrays.asList(ConfigType.values()));
                        System.exit(1);
                        return;
                }
            } catch (Exception exception) {
                log.error("Invalid config type '{}'. Allowed values are {}", type, Arrays.asList(ConfigType.values()));
                System.exit(1);
            }
        }

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().pipeline(builder.build()).build();

        if(yaml) {
            printOrSave(inferenceConfiguration.toYaml());
        } else {
            JsonObject output = new JsonObject(inferenceConfiguration.toJson());

            if (minified) {
                printOrSave(output.encode());
            } else {
                printOrSave(output.encodePrettily());
            }
        }
    }

    private PipelineStep logging() {
        return LoggingPipelineStep.builder().build();
    }

    private void printOrSave(String output) {
        if(outputFile == null) {
            out.println(output);
        } else {
            try {
                FileUtils.writeStringToFile(outputFile, output, StandardCharsets.UTF_8);
                out.format("Config file created successfully at %s%n", outputFile.getAbsolutePath());
            } catch (IOException exception) {
                log.error("Unable to save configuration file to {}", outputFile.getAbsolutePath(), exception);
            }
        }
    }
}
