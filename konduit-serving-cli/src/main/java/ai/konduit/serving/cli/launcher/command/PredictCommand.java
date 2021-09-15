/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
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

import ai.konduit.serving.cli.launcher.KonduitServingLauncher;
import ai.konduit.serving.cli.launcher.LauncherUtils;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage;
import ai.konduit.serving.pipeline.settings.DirectoryFetcher;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.ServerProtocol;
import ai.konduit.serving.vertx.protocols.grpc.api.InferenceGrpc;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.spi.launcher.DefaultCommand;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static ai.konduit.serving.cli.launcher.LauncherUtils.getPidFromServerId;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

@Name("predict")
@Summary("Run inference on konduit servers using given inputs")
@Description("Sends file data or JSON input data to running konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Sends input as json string to server with an id of 'inf_server' \n" +
        "  and fetches a json output:\n" +
        "$ konduit predict inf_server \"{...}\"\n\n" +
        "- Sends input from json file, 'file.json', to server with an id of 'inf_server' \n" +
        "  and fetches a json output:\n" +
        "$ konduit predict inf_server -it json-file file.json\n\n" +
        "- Sends input as binary from a string to server with an id of 'inf_server' \n" +
        "  and fetches a json output:\n" +
        "$ konduit predict inf_server -it binary <string>\n\n" +
        "- Sends input as multipart from an image to server with an id of 'inf_server' \n" +
        "  and fetches a json output:\n" +
        "$ konduit predict inf_server -it multipart \"image=@image.jpg;key2=key2-value\"\n\n" +
        "- Sends input as binary from file string, 'file.bin', to server with an id of \n" +
        "  'inf_server' using gRPC protocol and fetches a binary output:\n" +
        "$ konduit predict inf_server -it binary-file -ot binary <string>\n" +
        "--------------")
public class PredictCommand extends DefaultCommand {

    private static final String DEFAULT_INPUT_TYPE = "json";
    private static final String DEFAULT_OUTPUT_TYPE = "json";
    private static final List<String> VALID_INPUT_TYPES = Arrays.asList("json", "json-file", "binary", "binary-file", "multipart");
    private static final List<String> VALID_OUTPUT_TYPES = Arrays.asList("json", "binary");
    private static final String publishTopicName = "inference";
    private static final String subscribeTopicName = "inference-out";

    private String id;
    private String data;
    private String inputType = DEFAULT_INPUT_TYPE;
    private String outputType = DEFAULT_OUTPUT_TYPE;
    private int repeat = 1;
    private boolean debug = false;
    private int sentTimes = 0;
    private long startTimeForRequest = -1;
    private int mqttMaxInFlightQueue = 1000;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Argument(index = 1, argName = "data")
    @Description("Data to send to the server. Accepts JSON/Binary data string as the input")
    public void setData(String data) {
        this.data = data;
    }

    @Option(longName = "input-type", shortName = "it")
    @Description("Input type. " +
            "Choices are: [json, json-file, binary, binary-file, multipart]. Default is: '" + DEFAULT_INPUT_TYPE + "'")
    public void setInputType(String inputType) {
        if (VALID_INPUT_TYPES.contains(inputType)) {
            this.inputType = inputType;
        } else {
            System.out.format("Invalid input type: %s. Should be one of %s%n", inputType, VALID_INPUT_TYPES);
            System.exit(1);
        }
    }

    @DefaultValue("1")
    @Option(longName = "repeat", shortName = "r")
    @Description("Repeat requests the given number of times")
    public void setRepeat(int repeat) {
        if (repeat > 0) {
            this.repeat = repeat;
        } else {
            System.out.format("Given Number of repeated requests '%s' should be greater than 0%n", repeat);
            System.exit(1);
        }
    }

    @Option(longName = "debug", shortName = "d", flag = true)
    @Description("Debug with additional output")
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Option(longName = "output-type", shortName = "ot")
    @Description("Output type. " +
            "Choices are: [json, binary]. Default is: '" + DEFAULT_OUTPUT_TYPE + "'")
    public void setOutputType(String outputType) {
        if (VALID_OUTPUT_TYPES.contains(outputType)) {
            this.outputType = outputType;
        } else {
            System.out.format("Invalid output type: %s. Should be one of %s%n", outputType, VALID_OUTPUT_TYPES);
            System.exit(1);
        }
    }

    @DefaultValue("1000")
    @Option(longName = "mqtt-max-in-flight-queue", shortName = "mmifq")
    @Description("Maximum number of in-flight messages allowed for mqtt")
    public void setOutputType(int mqttMaxInFlightQueue) {
        if (mqttMaxInFlightQueue > 0) {
            this.mqttMaxInFlightQueue = mqttMaxInFlightQueue;
        } else {
            System.out.format("Given MQTT max in flight queue '%s' should be greater than 0%n", mqttMaxInFlightQueue);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        if (LauncherUtils.isProcessExists(id)) {
            Vertx vertx = Vertx.vertx();

            try {
                InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                        FileUtils.readFileToString(new File(DirectoryFetcher.getServersDataDir(),
                                getPidFromServerId(id) + ".data"), StandardCharsets.UTF_8));

                ServerProtocol protocol = inferenceConfiguration.protocol();

                switch (protocol) {
                    case HTTP:
                        String contentType;
                        if (inputType.contains("json")) {
                            contentType = APPLICATION_JSON.toString();
                        } else if (inputType.contains("multipart")) {
                            contentType = MULTIPART_FORM_DATA.toString();
                        } else {
                            contentType = APPLICATION_OCTET_STREAM.toString();
                        }

                        String accept;
                        if (outputType.contains("json")) {
                            accept = APPLICATION_JSON.toString();
                        } else {
                            accept = APPLICATION_OCTET_STREAM.toString();
                        }

                        HttpRequest<Buffer> request = WebClient.create(vertx)
                                .head(inferenceConfiguration.port(),
                                        inferenceConfiguration.host(),
                                        "/predict")
                                .putHeader(CONTENT_TYPE.toString(), contentType)
                                .putHeader(ACCEPT.toString(), accept)
                                .method(HttpMethod.POST);

                        Handler<AsyncResult<HttpResponse<Buffer>>> responseHandler = handler -> {
                            ++sentTimes;

                            if (debug) {
                                out.format("%n---%nResponse # %s%n---%n", sentTimes);
                            }

                            if (handler.succeeded()) {
                                HttpResponse<Buffer> httpResponse = handler.result();
                                int statusCode = httpResponse.statusCode();
                                if (statusCode == 200) {
                                    out.print(handler.result().body());
                                } else {
                                    out.format("Request failed with status code: %s%nDetails: %s%n", statusCode,
                                            handler.result().bodyAsString());
                                }
                            } else {
                                out.format("Failed request.%nExecute '%s logs %s' to find the cause.%n",
                                        ((KonduitServingLauncher) executionContext.launcher()).commandLinePrefix(), id);
                            }

                            if (repeat == sentTimes) {
                                if (debug) {
                                    out.format("%n---%nAverage time per response: %s (ms)%n---%n", (double) (System.currentTimeMillis() - startTimeForRequest) / sentTimes);
                                }

                                vertx.close();
                            }
                        };

                        if (inputType.contains("multipart")) {
                            MultipartForm multipartForm = MultipartForm.create();

                            if (data != null) {
                                for (String part : data.split(";")) {
                                    String[] partPair = part.split("=");
                                    if (partPair.length == 2) {
                                        String key = partPair[0];
                                        String value = partPair[1];

                                        if (value.startsWith("@")) {
                                            String filePath = value.substring(1);
                                            File file = new File(filePath);
                                            if (file.exists()) {
                                                multipartForm.binaryFileUpload(key, file.getName(), file.getAbsolutePath(), new Tika().detect(file));
                                            } else {
                                                out.format("File '%s' doesn't exist%n", filePath);
                                                vertx.close();
                                                return;
                                            }
                                        } else {
                                            multipartForm.attribute(key, value);
                                        }
                                    } else {
                                        out.format("The part pair '%s' should be in the format <key>=<value> for strings or <key>=@<value> for files%n", part);
                                        vertx.close();
                                        return;
                                    }
                                }
                            }

                            if (debug) {
                                out.format("Sending data:%n");
                                multipartForm.forEach(formDataPart ->
                                        out.format("---%n" +
                                                        "name: %s%n" +
                                                        "value: %s%n" +
                                                        "isFileUpload: %s%n" +
                                                        "isAttribute: %s%n" +
                                                        "isText: %s%n" +
                                                        "filename: %s%n" +
                                                        "mediaType: %s%n" +
                                                        "pathname: %s%n" +
                                                        "---%n",
                                                formDataPart.name(),
                                                formDataPart.value(),
                                                formDataPart.isFileUpload(),
                                                formDataPart.isAttribute(),
                                                formDataPart.isText(),
                                                formDataPart.filename(),
                                                formDataPart.mediaType(),
                                                formDataPart.pathname())
                                );

                                startTimeForRequest = System.currentTimeMillis();
                            }

                            for (int i = 0; i < repeat; i++) {
                                request.sendMultipartForm(multipartForm, responseHandler);
                            }
                        } else {
                            if (inputType.contains("file")) {
                                Buffer buffer = Buffer.buffer(FileUtils.readFileToByteArray(new File(data)));

                                if (debug) {
                                    out.format("Sending data: %s%n", buffer.toString(StandardCharsets.UTF_8.name()));
                                    startTimeForRequest = System.currentTimeMillis();
                                }

                                for (int i = 0; i < repeat; i++) {
                                    request.sendBuffer(buffer, responseHandler);
                                }
                            } else {
                                Buffer buffer = Buffer.buffer(data.getBytes());

                                if (debug) {
                                    out.format("Sending data: %s%n", buffer.toString(StandardCharsets.UTF_8.name()));
                                    startTimeForRequest = System.currentTimeMillis();
                                }

                                for (int i = 0; i < repeat; i++) {
                                    request.sendBuffer(buffer, responseHandler);
                                }
                            }
                        }
                        break;
                    case GRPC:
                        if (!VALID_INPUT_TYPES.contains(inputType)) {
                            out.format("Invalid input type %s for gRPC protocol valid input types are %s%n",
                                    inputType, VALID_INPUT_TYPES);
                            System.exit(1);
                        }

                        if (!VALID_OUTPUT_TYPES.contains(outputType)) {
                            out.format("Invalid output type %s for gRPC protocol valid output types are %s%n",
                                    outputType, VALID_OUTPUT_TYPES);
                            System.exit(1);
                        }

                        byte[] binaryData;

                        if (inputType.contains("file")) {
                            File inputFile = new File(data);
                            if (!inputFile.exists() || !inputFile.isFile()) {
                                out.format("Input file %s doesn't exist or isn't a file type.%n", data);
                                System.exit(1);
                            }

                            if (inputType.contains("json")) {
                                Data dataInstance = Data.empty();

                                try {
                                    dataInstance = Data.fromJson(
                                            FileUtils.readFileToString(inputFile, StandardCharsets.UTF_8)
                                    );

                                } catch (Exception exception) {
                                    out.format("Error while parsing input json from file. Details %s%n", exception.getMessage());
                                    System.exit(1);
                                }

                                binaryData = dataInstance.asBytes();
                            } else {
                                binaryData = FileUtils.readFileToByteArray(inputFile);
                            }
                        } else {
                            if (inputType.contains("json")) {
                                Data dataInstance = Data.empty();

                                try {
                                    dataInstance = Data.fromJson(data);

                                } catch (Exception exception) {
                                    out.format("Error while parsing input json from json string. Details %s%n", exception.getMessage());
                                    System.exit(1);
                                }

                                binaryData = dataInstance.asBytes();
                            } else {
                                binaryData = data.getBytes();
                            }
                        }

                        InferenceGrpc.InferenceVertxStub inferenceVertxStub = InferenceGrpc.newVertxStub(VertxChannelBuilder
                                .forAddress(vertx, inferenceConfiguration.host(), inferenceConfiguration.port())
                                .usePlaintext(true)
                                .build());

                        if(debug) {
                            startTimeForRequest = System.currentTimeMillis();

                            out.format("Sending data: %s%n", new String(binaryData));
                        }

                        for (int i = 0; i < repeat; i++) {
                            inferenceVertxStub.predict(DataProtoMessage.DataScheme.parseFrom(binaryData),
                                    ar -> {
                                        sentTimes++;

                                        if (debug) {
                                            out.format("%n---%nResponse # %s%n---%n", sentTimes);
                                            out.println("---");
                                        }

                                        if (ar.succeeded()) {
                                            if (outputType.contains("json")) {
                                                out.print(Data.fromBytes(ar.result().toByteArray()).toJson());
                                            } else {
                                                out.print(new String(ar.result().toByteArray()));
                                            }
                                        } else {
                                            ar.cause().printStackTrace(out);
                                        }

                                        if (sentTimes == repeat) {
                                            if (debug) {
                                                out.format("%n---%nAverage time per response: %s (ms)%n---%n", (double) (System.currentTimeMillis() - startTimeForRequest) / sentTimes);
                                            }

                                            vertx.close();
                                        }
                                    });
                        }
                        break;
                    case MQTT:
                        if (!VALID_INPUT_TYPES.contains(inputType)) {
                            out.format("Invalid input type %s for MQTT protocol valid input types are %s%n",
                                    inputType, VALID_INPUT_TYPES);
                            System.exit(1);
                        }

                        if (!VALID_OUTPUT_TYPES.contains(outputType)) {
                            out.format("Invalid output type %s for MQTT protocol valid output types are %s%n",
                                    outputType, VALID_OUTPUT_TYPES);
                            System.exit(1);
                        }

                        String jsonData;

                        if (inputType.contains("file")) {
                            File inputFile = new File(data);
                            if (!inputFile.exists() || !inputFile.isFile()) {
                                out.format("Input file %s doesn't exist or isn't a file type.%n", data);
                                System.exit(1);
                            }

                            if (inputType.contains("json")) {
                                jsonData = FileUtils.readFileToString(inputFile, StandardCharsets.UTF_8);
                            } else {
                                Data dataInstance = Data.empty();

                                try {
                                    dataInstance = Data.fromBytes(FileUtils.readFileToByteArray(inputFile));
                                } catch (Exception exception) {
                                    out.format("Error creating a data instance from binary file. Details: %s%n", exception.getMessage());
                                    System.exit(1);
                                }

                                jsonData = dataInstance.toJson();
                            }
                        } else {
                            if (inputType.contains("json")) {
                                jsonData = data;
                            } else {
                                Data dataInstance = Data.empty();

                                try {
                                    dataInstance = Data.fromBytes(data.getBytes());
                                } catch (Exception exception) {
                                    out.format("Error creating a data instance from binary string. Details: %s%n", exception.getMessage());
                                    System.exit(1);
                                }

                                jsonData = dataInstance.toJson();
                            }
                        }

                        if(debug) {
                            startTimeForRequest = System.currentTimeMillis();

                            out.format("Sending data: %s", jsonData);
                        }

                        MqttClient client = MqttClient.create(vertx, new MqttClientOptions().setMaxInflightQueue(mqttMaxInFlightQueue));

                        client.connect(inferenceConfiguration.port(), inferenceConfiguration.host(), connectHandler -> {
                            client.publishHandler(
                                    publishHandler -> {
                                        sentTimes++;

                                        if (debug) {
                                            out.format("%n---%nResponse # %s%n---%n", sentTimes);

                                            out.println("There are new message in topic: " + publishHandler.topicName());
                                            out.println("QoS: " + publishHandler.qosLevel());
                                            out.println("---");
                                        }

                                        if (outputType.contains("json")) {
                                            out.print(publishHandler.payload().toString());
                                        } else {
                                            out.print(new String(publishHandler.payload().getBytes()));
                                        }

                                        if (sentTimes == repeat) {
                                            if (debug) {
                                                out.format("%n---%nAverage time per response: %s (ms)%n---%n", (double) (System.currentTimeMillis() - startTimeForRequest) / sentTimes);
                                            }

                                            vertx.close();
                                        }
                                    })
                                    .subscribe(subscribeTopicName, MqttQoS.EXACTLY_ONCE.value(), subscribeHandler -> {
                                        if (subscribeHandler.succeeded()) {
                                            if (debug) {
                                                out.println("Subscribed successfully to '" + subscribeTopicName + "' topic");
                                            }
                                        } else {
                                            subscribeHandler.cause().printStackTrace();
                                            vertx.close();
                                        }
                                    });

                            for (int i = 0; i < repeat; i++) {
                                client.publish(publishTopicName,
                                        Buffer.buffer(jsonData),
                                        MqttQoS.EXACTLY_ONCE,
                                        false,
                                        false);
                            }
                        });
                        break;
                    default:
                        throw new CLIException(String.format("Unsupported protocol with the 'predict' command: %s. " +
                                        "Only [HTTP, GRPC, MQTT] are supported.",
                                protocol));
                }
            } catch (Exception exception) {
                exception.printStackTrace(out);
                vertx.close();
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }
}