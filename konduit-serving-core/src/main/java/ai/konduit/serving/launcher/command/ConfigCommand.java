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
import ai.konduit.serving.model.*;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import io.vertx.core.cli.CLIException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Name("config")
@Summary("A helper command for creating JSON for inference configuration")
@Description("This command is a utility to create json configurations that can be consumed to start konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Prints 'tensorflow' config in pretty format:\n" +
        "$ konduit config -t tensorflow\n\n" +
        "- Prints 'image + dl4j' config in minified format:\n" +
        "$ konduit config -t image,dl4j -m\n\n" +
        "- Saves 'image + dl4j' config in a 'config.json' file:\n" +
        "$ konduit config -t image,dl4j -o config.json\n" +
        "--------------")
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

    private String types;
    private boolean minified;
    private File outputFile;

    @Option(longName = "types", shortName = "t", argName = "config-types", required = true)
    @Description("A comma-separated list of pipeline steps you want to create boilerplate configuration for. Allowed values are: [image, python, tensorflow, onnx, pmml, dl4j, keras]")
    public void setTypes(String types) {
        this.types = types;
    }

    @Option(longName = "minified", shortName = "m", flag = true)
    @Description("If set, the output json will be printed in a single line, without indentations.")
    public void setMinified(boolean minified) {
        this.minified = minified;
    }

    @Option(longName = "output", shortName = "o", argName = "output-file")
    @Description("Optional: If set, the generated json will be saved here. Otherwise, it's printed on the console.")
    public void setOutputFile(String output) {
        outputFile = new File(output);
        if(outputFile.exists()) {
            if(!outputFile.isFile()) {
                log.error(String.format("'%s' is not a valid file location", outputFile));
            }
        } else {
            try {
                if(!outputFile.createNewFile()) {
                    log.error(String.format("'%s' is not a valid file location", outputFile));
                }
            } catch (Exception exception) {
                log.error(String.format("Error while creating file: '%s'", outputFile), exception);
            }
        }
    }

    @Override
    public void run() throws CLIException {
        List<PipelineStep> pipelineSteps = new ArrayList<>();

        for (String type : types.split(",")) {
            try {
                switch (ConfigType.valueOf(type.trim())) {
                    case image:
                        pipelineSteps.add(image());
                        break;
                    case python:
                        pipelineSteps.add(python());
                        break;
                    case tensorflow:
                        pipelineSteps.add(tensorflow());
                        break;
                    case onnx:
                        pipelineSteps.add(onnx());
                        break;
                    case pmml:
                        pipelineSteps.add(pmml());
                        break;
                    case dl4j:
                        pipelineSteps.add(dl4j());
                        break;
                    case keras:
                        pipelineSteps.add(keras());
                        break;
                    default:
                        log.error(String.format("Invalid config type '%s'. Allowed values are %s", type, Arrays.asList(ConfigType.values())));
                        System.exit(1);
                        return;
                }
            } catch (Exception exception) {
                log.error(String.format("Invalid config type '%s'. Allowed values are %s", type, Arrays.asList(ConfigType.values())));
                System.exit(1);
            }
        }

        JsonObject output = new JsonObject(InferenceConfiguration.builder()
                .servingConfig(ServingConfig.builder().build())
                .steps(pipelineSteps).build().toJson());

        if (minified) {
            printOrSave(output.encode());
        } else {
            printOrSave(output.encodePrettily());
        }
    }

    private PipelineStep<ImageLoadingStep> image() {
        return ImageLoadingStep.builder()
                .inputName("default")
                .outputName("default")
                .build();
    }

    private PipelineStep<PythonStep> python() {
        return PythonStep.builder()
                .inputName("default")
                .outputName("default")
                .build();
    }

    private PipelineStep<ModelStep> tensorflow() {
        return ModelStep.builder()
                .inputName("default")
                .outputName("default")
                .modelConfig(
                        TensorFlowConfig.builder()
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> onnx() {
        return ModelStep.builder()
                .inputName("default")
                .outputName("default")
                .modelConfig(
                        OnnxConfig.builder()
                                .build())
                .build();
    }

    private PipelineStep<ModelStep> pmml() {
        return ModelStep.builder()
                .inputName("default")
                .outputName("default")
                .modelConfig(
                        PmmlConfig.builder()
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> dl4j() {
        return ModelStep.builder()
                .inputName("default")
                .outputName("default")
                .modelConfig(
                        DL4JConfig.builder()
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> keras() {
        return ModelStep.builder()
                .inputName("default")
                .outputName("default")
                .modelConfig(
                        KerasConfig.builder()
                                .build()
                )
                .build();
    }

    private void printOrSave(String output) {
        if(outputFile == null) {
            log.info(output);
        } else {
            try {
                FileUtils.writeStringToFile(outputFile, output, StandardCharsets.UTF_8);
                log.info("Config file created successfully at {}", outputFile.getAbsolutePath());
            } catch (IOException exception) {
                log.error(String.format("Unable to save configuration file to %s", outputFile.getAbsolutePath()), exception);
            }
        }
    }
}
