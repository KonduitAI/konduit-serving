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
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.datavec.python.PythonType;

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

    public static final String DEFAULT = "default";

    private enum ConfigType {
        IMAGE,
        PYTHON,
        TENSORFLOW,
        ONNX,
        PMML,
        DL4J,
        KERAS
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
        List<PipelineStep> pipelineSteps = new ArrayList<>();

        for (String type : types.split(",")) {
            try {
                switch (ConfigType.valueOf(type.trim())) {
                    case IMAGE:
                        pipelineSteps.add(image());
                        break;
                    case PYTHON:
                        pipelineSteps.add(python());
                        break;
                    case TENSORFLOW:
                        pipelineSteps.add(tensorflow());
                        break;
                    case ONNX:
                        pipelineSteps.add(onnx());
                        break;
                    case PMML:
                        pipelineSteps.add(pmml());
                        break;
                    case DL4J:
                        pipelineSteps.add(dl4j());
                        break;
                    case KERAS:
                        pipelineSteps.add(keras());
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
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .build();
    }

    private PipelineStep<PythonStep> python() {
        return PythonStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .pythonConfig(DEFAULT,
                        PythonConfig.builder()
                                .pythonInput("x1", PythonType.TypeName.NDARRAY.name())
                                .pythonInput("x2", PythonType.TypeName.NDARRAY.name())
                                .pythonOutput("y1", PythonType.TypeName.NDARRAY.name())
                                .pythonOutput("y2", PythonType.TypeName.NDARRAY.name())
                                .pythonPath("# Execute <python -c 'import sys, os; print(os.pathsep.join([path for path in sys.path if path]))'> to find the value of it.")
                                .pythonCode("<python-script # Remove this if 'pythonCodePath' is set>")
                                .pythonCodePath("<python-code-path # Remove this if 'pythonCode' is set>")
                                .build())
                .build();
    }

    private PipelineStep<ModelStep> tensorflow() {
        return ModelStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .modelConfig(
                        TensorFlowConfig.builder()
                                .modelConfigType(ModelConfigType.tensorFlow("<path-to-the-tensorflow-model>"))
                                .tensorDataTypesConfig(TensorDataTypesConfig.builder()
                                        .inputDataType(DEFAULT, TensorDataType.FLOAT)
                                        .outputDataType(DEFAULT, TensorDataType.FLOAT)
                                        .build())
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> onnx() {
        return ModelStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .modelConfig(
                        OnnxConfig.builder()
                                .modelConfigType(ModelConfigType.onnx("<path-to-the-onnx-model>"))
                                .build())
                .build();
    }

    private PipelineStep<ModelStep> pmml() {
        return ModelStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .modelConfig(
                        PmmlConfig.builder()
                                .modelConfigType(ModelConfigType.pmml("<path-to-the-pmml-model"))
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> dl4j() {
        return ModelStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .modelConfig(
                        DL4JConfig.builder()
                                .modelConfigType(ModelConfigType.dl4j("<path-to-the-dl4j-model"))
                                .build()
                )
                .build();
    }

    private PipelineStep<ModelStep> keras() {
        return ModelStep.builder()
                .inputName(DEFAULT)
                .outputName(DEFAULT)
                .modelConfig(
                        KerasConfig.builder()
                                .modelConfigType(ModelConfigType.keras("<path-to-the-keras-model"))
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
                log.error("Unable to save configuration file to {}", outputFile.getAbsolutePath(), exception);
            }
        }
    }
}
