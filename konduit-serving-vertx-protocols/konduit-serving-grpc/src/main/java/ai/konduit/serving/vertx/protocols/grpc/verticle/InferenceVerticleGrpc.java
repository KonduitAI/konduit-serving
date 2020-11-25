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

package ai.konduit.serving.vertx.protocols.grpc.verticle;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.ProtoData;
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme;
import ai.konduit.serving.vertx.protocols.grpc.api.InferenceGrpc;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InferenceVerticleGrpc extends InferenceVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.executeBlocking(handler -> {
            try {
                initialize();
                handler.complete();
            } catch (Exception exception) {
                handler.fail(exception);
                startPromise.fail(exception);
            }
        }, resultHandler -> {
            if(resultHandler.failed()) {
                if(resultHandler.cause() != null)
                    startPromise.fail(resultHandler.cause());
                else {
                    startPromise.fail("Failed to start. Unknown cause.");
                }
            }
            else {

                int port;

                String portEnvValue = System.getenv(EnvironmentConstants.KONDUIT_SERVING_PORT);
                if (portEnvValue != null) {
                    try {
                        port = Integer.parseInt(portEnvValue);
                    } catch (NumberFormatException exception) {
                        log.error("Environment variable \"{}={}\" isn't a valid port number.",
                                EnvironmentConstants.KONDUIT_SERVING_PORT, portEnvValue);
                        startPromise.fail(exception);
                        return;
                    }
                } else {
                    port = inferenceConfiguration.port();
                }

                if (port < 0 || port > 0xFFFF) {
                    startPromise.fail(new Exception("Valid port range is 0 <= port <= 65535. The given port was " + port));
                    return;
                }

                VertxServer rpcServer = VertxServerBuilder
                        .forAddress(vertx, inferenceConfiguration.host(), inferenceConfiguration.port())
                        .addService(new InferenceGrpc.InferenceImplBase() {
                            @Override
                            public void predict(DataScheme request, StreamObserver<DataScheme> responseObserver) {
                                try {
                                    Data output = pipelineExecutor.exec(ProtoData.fromBytes(request.toByteArray()));
                                    responseObserver.onNext(DataScheme.parseFrom(output.asBytes()));
                                    responseObserver.onCompleted();
                                } catch (Throwable throwable) {
                                    log.error("Failed to process the pipeline with the input data", throwable);
                                    responseObserver.onError(throwable);
                                }
                            }
                        })
                        .build();

                rpcServer.start(handler -> {
                    if(handler.succeeded()) {
                        int actualPort = rpcServer.getPort();

                        inferenceConfiguration.port(actualPort);

                        try {
                            ((ContextInternal) context).getDeployment()
                                    .deploymentOptions()
                                    .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                            long pid = getPid();

                            saveInspectionDataIfRequired(pid);

                            log.info("Inference gRPC server is listening on host: '{}'", inferenceConfiguration.host());
                            log.info("Inference gRPC server started on port {} with {} pipeline steps", actualPort, pipeline.size());
                            startPromise.complete();
                        } catch (Throwable throwable) {
                            startPromise.fail(throwable);
                        }
                    } else {
                        startPromise.fail(handler.cause());
                    }
                });
            }
        });


    }
}
