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

package ai.konduit.serving.launcher.command;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.model.KerasConfig;
import ai.konduit.serving.model.PmmlConfig;
import ai.konduit.serving.model.TensorFlowConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Name("config")
@Summary("A helper command for creating JSON inference configuration")
@Description("This command is a utility to create configurations that can be used to start konduit servers.")
@Slf4j
public class ConfigCommand extends DefaultCommand {

    private enum ConfigType {
        image,
        python,
        tensorflow,
        onnx,
        pmml,
        dl4j,
        keras
    }

    private ConfigType type;
    private boolean pretty;
    private File outputFile;

    @Option(longName = "type", shortName = "t", argName = "config-type")
    @DefaultValue("image")
    @Description("Type of configuration you want to create boilerplate for.")
    public void setType(String type) {
        try {
            this.type = ConfigType.valueOf(type);
        } catch (Exception exception) {
            log.error(String.format("Invalid config type \'%s\'. Allowed values are %s", type, Arrays.asList(ConfigType.values())));
            System.exit(1);
        }
    }

    @Option(longName = "pretty", shortName = "p", flag = true)
    @Description("If set, the output json will be generated in a pretty format.")
    public void setPretty(boolean pretty) {
        this.pretty = pretty;
    }

    @Option(longName = "output", shortName = "o", argName = "output-file")
    @Description("Optional: If set, the generated json will be saved here. Otherwise, it's printed on the console.")
    public void setOutputFile(String output) {
        outputFile = new File(output);
        if(outputFile.exists()) {
            if(!outputFile.isFile()) {
                log.error(String.format("\'%s\' is not a valid file location", outputFile));
            }
        } else {
            try {
                if(!outputFile.createNewFile()) {
                    log.error(String.format("\'%s\' is not a valid file location", outputFile));
                }
            } catch (Exception exception) {
                log.error(String.format("Error while creating file: \'%s\'", outputFile), exception);
            }
        }
    }

    @Override
    public void run() throws CLIException {
        JsonObject output;

        switch (type) {
            case image:
                output = image();
                break;
            case python:
                output = python();
                break;
            case tensorflow:
                output = tensorflow();
                break;
            case onnx:
                output = onnx();
                break;
            case pmml:
                output = pmml();
                break;
            case dl4j:
                output = dl4j();
                break;
            case keras:
                output = keras();
                break;
            default:
                System.exit(1);
                return;
        }

        if (pretty) {
            printOrSave(output.encodePrettily());
        } else {
            printOrSave(output.encode());
        }
    }

    private JsonObject image() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ImageLoadingStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject python() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(PythonStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject tensorflow() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ModelStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .modelConfig(
                                        TensorFlowConfig.builder()
                                        .build()
                                )
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject onnx() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ModelStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject pmml() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ModelStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .modelConfig(
                                        PmmlConfig.builder()
                                                .build()
                                )
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject dl4j() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ModelStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .modelConfig(
                                        DL4JConfig.builder()
                                                .build()
                                )
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private JsonObject keras() {
        return new JsonObject(
                new InferenceConfiguration(
                        Arrays.asList(ModelStep.builder()
                                .inputName("default")
                                .outputName("default")
                                .modelConfig(
                                        KerasConfig.builder()
                                                .build()
                                )
                                .build()),
                        ServingConfig.builder()
                                .build(),
                        null
                ).toJson()
        );
    }

    private void printOrSave(String output) {
        if(outputFile == null) {
            log.info(output);
        } else {
            try {
                FileUtils.writeStringToFile(outputFile, output, StandardCharsets.UTF_8);
                log.info("Config file create successfully at {}", outputFile.getAbsolutePath());
            } catch (IOException exception) {
                log.error(String.format("Unable to save configuration file to %s", outputFile.getAbsolutePath()), exception);
            }
        }
    }
}
