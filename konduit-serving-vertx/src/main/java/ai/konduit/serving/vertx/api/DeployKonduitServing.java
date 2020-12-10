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

import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.util.ObjectMappers;
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
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ai.konduit.serving.vertx.config.ServerProtocol.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeployKonduitServing {
    public static final String SERVICE_PREFIX = "konduit";
    public static final String INFERENCE_SERVICE_IDENTIFIER = SERVICE_PREFIX + ":ai.konduit.serving:inference";
    protected static final Map<ServerProtocol, String> PROTOCOL_SERVICE_MAP = new EnumMap<>(ServerProtocol.class);

    static {
        ObjectMappers.json().setDateFormat(new SimpleDateFormat(Constants.DATE_FORMAT));

        // Service classes that corresponds to the ServerProtocol enums
        PROTOCOL_SERVICE_MAP.put(HTTP, "ai.konduit.serving.vertx.protocols.http.verticle.InferenceVerticleHttp");
        PROTOCOL_SERVICE_MAP.put(GRPC, "ai.konduit.serving.vertx.protocols.grpc.verticle.InferenceVerticleGrpc");
        PROTOCOL_SERVICE_MAP.put(MQTT, "ai.konduit.serving.vertx.protocols.mqtt.verticle.InferenceVerticleMqtt");
        PROTOCOL_SERVICE_MAP.put(KAFKA, "ai.konduit.serving.vertx.protocols.kafka.verticle.InferenceVerticleKafka");
    }

    public static Vertx deploy(VertxOptions vertxOptions,
                              DeploymentOptions deploymentOptions,
                              InferenceConfiguration inferenceConfiguration,
                              Handler<AsyncResult<InferenceDeploymentResult>> eventHandler) {
        Vertx vertx = Vertx.vertx(vertxOptions
                .setMaxEventLoopExecuteTime(10)
                .setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS));
        registerInferenceVerticleFactory(vertx);

        JsonObject jsonConfiguration;

        try {
            jsonConfiguration = new JsonObject(inferenceConfiguration.toJson());
        } catch (Throwable throwable) {
            log.error("Failed while converting inference configuration to a json string." +
                    "Possible causes could be missing classes for pipeline steps.", throwable);
            eventHandler.handle(Future.failedFuture(throwable));
            vertx.close();
            return vertx;
        }

        deploymentOptions.setConfig(jsonConfiguration);

        vertx.deployVerticle(INFERENCE_SERVICE_IDENTIFIER + ":" +
                        inferenceConfiguration.protocol().name().toLowerCase(),
                deploymentOptions, handler -> {
            if (handler.failed()) {
                log.error("Unable to deploy server for configuration \n{}",
                        jsonConfiguration.encodePrettily(), handler.cause());

                if (eventHandler != null) {
                    eventHandler.handle(Future.failedFuture(handler.cause()));
                }

                vertx.close();
            } else {
                log.info("Deployed {} server with configuration \n{}",
                        inferenceConfiguration.protocol(),
                        jsonConfiguration.encodePrettily());

                if (eventHandler != null) {
                    VertxImpl vertxImpl = (VertxImpl) vertx;
                    DeploymentOptions inferenceDeploymentOptions = vertxImpl.getDeployment(handler.result()).deploymentOptions();

                    eventHandler.handle(Future.succeededFuture(
                            new InferenceDeploymentResult(
                                    inferenceDeploymentOptions.getConfig().getInteger("port"),
                                    handler.result()
                            )));
                }
            }
        });

        return vertx;
    }

    public Map<ServerProtocol, String> getProtocolServiceMap() {
        return PROTOCOL_SERVICE_MAP;
    }

    public static void registerInferenceVerticleFactory(Vertx vertx) {
        vertx.registerVerticleFactory(new ServiceVerticleFactory(vertx));
    }
}
