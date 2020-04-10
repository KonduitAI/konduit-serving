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

package ai.konduit.serving.launcher.command;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.launcher.KonduitServingLauncher;
import ai.konduit.serving.launcher.LauncherUtils;
import ai.konduit.serving.settings.Fetcher;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static ai.konduit.serving.launcher.LauncherUtils.getPidFromServerId;

@Slf4j
@Name("predict")
@Summary("Run inference on konduit servers using given inputs")
@Description("Sends file data or JSON input data to running konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Sends input as JSON string to server with an id of 'inf_server' for processing:\n" +
        "$ konduit predict inf_server \"{...}\"\n\n" +
        "- Sends input (image.jpg) as 'image' to server with an id of 'inf_server' for processing:\n" +
        "$ konduit predict -it IMAGE inf_server image.jpg\n\n" +
        "- Sends input (input.npy) as 'numpy array binary' to server with an id of 'inf_server' for classification:\n" +
        "$ konduit predict -it NUMPY -pt CLASSIFICATION inf_server input.npy\n\n" +
        "- Sends inputs (input1.zip and input2.zip) as 'nd4j array binaries' to server with an id of 'inf_server' for classification:\n" +
        "$ konduit predict -it ND4J -pt CLASSIFICATION inf_server input1.zip,input2.zip\n" +
        "--------------")
public class PredictCommand extends DefaultCommand {

    String id;
    String data;
    Input.DataFormat inputDataFormat = Input.DataFormat.JSON;
    Output.PredictionType predictionType = Output.PredictionType.RAW;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Argument(index = 1, argName = "data")
    @Description("Data to send to the server. Accepts JSON string, or a list of comma-separated file paths (corresponding to configured input names), as input data")
    public void setData(String data) {
        this.data = data;
    }

    @Option(longName = "input-type", shortName = "it")
    @Description("Input type. Choices are: [NUMPY,JSON,ND4J,IMAGE,ARROW]")
    public void setInputType(String inputType) {
        try {
        inputDataFormat = Input.DataFormat.valueOf(inputType);
        } catch (Exception exception) {
            log.error("Input type should be one of {}", Arrays.asList(Input.DataFormat.values()));
            System.exit(1);
        }
    }

    @Option(longName = "prediction-type", shortName = "pt")
    @Description("Prediction type. Choices are: [CLASSIFICATION,YOLO,SSD,RCNN,RAW,REGRESSION]")
    public void setPredictionType(String predictionType) {
        try {
            this.predictionType = Output.PredictionType.valueOf(predictionType);
        } catch (Exception exception) {
            log.error("Prediction type should be one of {}", Arrays.asList(Output.PredictionType.values()));
            System.exit(1);
        }
    }

    @Override
    public void run() throws CLIException {
        if(LauncherUtils.isProcessExists(id)) {
            try {
                InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                        FileUtils.readFileToString(new File(Fetcher.getServersDataDir(),
                                getPidFromServerId(id) + ".data"), StandardCharsets.UTF_8));

                Vertx vertx = Vertx.vertx();

                HttpRequest<Buffer> request = WebClient.create(vertx)
                        .post(inferenceConfiguration.getServingConfig().getHttpPort(),
                                inferenceConfiguration.getServingConfig().getListenHost(),
                                String.format("/%s/%s", predictionType, inputDataFormat));

                Handler<AsyncResult<HttpResponse<Buffer>>> responseHandler = handler -> {
                    boolean failed = false;
                    if(handler.succeeded()) {
                        String body = handler.result().bodyAsString();
                        if(body == null) {
                            out.format("Failed request.\nExecute '%s logs %s' to find the cause.\n",
                                    ((KonduitServingLauncher) executionContext.launcher()).commandLinePrefix(),
                                    id);
                            failed = true;
                        } else {
                            out.println(handler.result().body());
                        }
                    } else {
                        out.println("Request failed: " + handler.cause().getMessage());
                        failed = true;
                    }

                    vertx.close();

                    if(failed) {
                        System.exit(1);
                    }
                };

                JsonObject jsonData = null;
                if (inputDataFormat == Input.DataFormat.JSON) {
                    try {
                        jsonData = new JsonObject(data);
                    } catch (Exception exception) {
                        log.info("Unable to parse input data as json. Reason: " + exception.getMessage());
                        System.exit(1);
                    }

                    request.sendJsonObject(jsonData, responseHandler);
                } else {
                    String[] filePaths = data.split(",");
                    List<String> inputNames = inferenceConfiguration.getSteps().get(0).getInputNames();

                    MultipartForm multipartForm = MultipartForm.create();

                    if (filePaths.length != inputNames.size()) {
                        log.error("Number of provided data files ({}) are not equal to the number of inputs required ({}). " +
                                "Input names are: {}", filePaths.length, inputNames.size(), inputNames);
                        System.exit(1);
                    }

                    for (int i = 0; i < filePaths.length; i++) {
                        File file = new File(filePaths[i]);
                        if (!file.exists()) {
                            log.error("Input file '{}' doesn't exist", file.getAbsolutePath());
                            System.exit(1);
                        }

                        multipartForm.binaryFileUpload(inputNames.get(i),
                                Paths.get(filePaths[i]).getFileName().toString(),
                                filePaths[i],
                                "application/x-binary");
                    }

                    request.sendMultipartForm(multipartForm, responseHandler);
                }
            } catch (Exception exception) {
                log.error("Failed to read configuration file", exception);
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }
}