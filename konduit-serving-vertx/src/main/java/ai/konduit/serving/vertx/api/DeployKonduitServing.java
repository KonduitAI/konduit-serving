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

package ai.konduit.serving.vertx.api;

import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import io.vertx.core.*;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeployKonduitServing {
    public static final String SERVICE_PREFIX = "konduit";
    public static final String INFERENCE_SERVICE_IDENTIFIER = SERVICE_PREFIX + ":ai.konduit.serving:inference";

    public static void deploy(VertxOptions vertxOptions,
                              DeploymentOptions deploymentOptions,
                              InferenceConfiguration inferenceConfiguration,
                              Handler<AsyncResult<InferenceDeploymentResult>> eventHandler) {
        Vertx vertx = Vertx.vertx(vertxOptions
                .setMaxEventLoopExecuteTime(10)
                .setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS));
        vertx.registerVerticleFactory(new VerticleFactory() {
            @Override
            public String prefix() {
                return SERVICE_PREFIX;
            }

            @Override
            public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
                    String protocolName = verticleName.substring(verticleName.lastIndexOf(":") + 1);

                    try {
                        switch (ServerProtocol.valueOf(protocolName.toUpperCase())) {
                            case HTTP:
                                return createVerticleFromClassString("ai.konduit.serving.vertx.protocols.http.verticle.InferenceVerticleHttp");
                            case GRPC:
                                return createVerticleFromClassString("ai.konduit.serving.vertx.protocols.grpc.verticle.InferenceVerticleGrpc");
                            case MQTT:
                                return createVerticleFromClassString("ai.konduit.serving.vertx.protocols.mqtt.verticle.InferenceVerticleMqtt");
                            default:
                                throw new IllegalStateException(
                                        String.format("Invalid service type %s. Possible values are: %s",
                                                protocolName,
                                                Arrays.toString(ServerProtocol.values())
                                        )
                                );
                        }
                    } catch (ClassNotFoundException classNotFoundException) {
                        // todo: write plus log a meaningful message and throw exception
                        throw new IllegalStateException();
                    }
            }

            private Verticle createVerticleFromClassString(String className) throws Exception {
                return (Verticle) ClassLoader.getSystemClassLoader()
                        .loadClass(className)
                        .getConstructor().newInstance();
            }
        });

        JsonObject jsonConfiguration = new JsonObject(inferenceConfiguration.toJson());
        deploymentOptions.setConfig(jsonConfiguration);

        vertx.deployVerticle(INFERENCE_SERVICE_IDENTIFIER + ":" +
                        inferenceConfiguration.getProtocol().name().toLowerCase(),
                deploymentOptions, handler -> {
            if (handler.failed()) {
                log.error("Unable to deploy server for configuration \n{}",
                        jsonConfiguration.encodePrettily(), handler.cause());

                if (eventHandler != null) {
                    eventHandler.handle(Future.failedFuture(handler.cause()));
                }

                vertx.close();
            } else {
                log.info("Deployed server with configuration \n{}",
                        jsonConfiguration.encodePrettily());

                if (eventHandler != null) {
                    VertxImpl vertxImpl = (VertxImpl) vertx;
                    DeploymentOptions inferenceDeploymentOptions = vertxImpl.getDeployment(handler.result()).deploymentOptions();

                    eventHandler.handle(Future.succeededFuture(
                            new InferenceDeploymentResult(
                                    inferenceDeploymentOptions.getConfig().getInteger("port")
                            )));
                }
            }
        });
    }
}
