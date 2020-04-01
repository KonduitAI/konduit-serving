/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.orchestration;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.util.ObjectMappers;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import lombok.extern.slf4j.Slf4j;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.file.FileSystemOptions.DEFAULT_FILE_CACHING_DIR;
import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static io.vertx.core.file.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

/**
 * Multi node/clustered setup using
 * {@link Vertx#clusteredVertx(VertxOptions, Handler) }
 * for initialization for multi node communication
 *
 * @author Adam Gibson
 */
@Slf4j
public class DeployKonduitOrchestration {

    static {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx

        setProperty("vertx.cwd", new File(".").getAbsolutePath());
        setProperty(CACHE_DIR_BASE_PROP_NAME, DEFAULT_FILE_CACHING_DIR);
        setProperty(DISABLE_CP_RESOLVING_PROP_NAME, Boolean.TRUE.toString());
    }

    public final static String NODE_COMMUNICATION_TOPIC = "NodeCommunication";

    public static void deployInferenceClustered(DeploymentOptions deploymentOptions, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        deployInferenceClustered(new VertxOptions().setMaxEventLoopExecuteTime(120).setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS),
                deploymentOptions, eventHandler);
    }

    public static void deployInferenceClustered(InferenceConfiguration inferenceConfiguration, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        deployInferenceClustered(new VertxOptions().setMaxEventLoopExecuteTime(120).setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS),
                new DeploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson())), eventHandler);
    }

    public static void deployInferenceClustered(VertxOptions vertxOptions, InferenceConfiguration inferenceConfiguration, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        deployInferenceClustered(vertxOptions, new DeploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson())), eventHandler);
    }

    public static void deployInferenceClustered(VertxOptions vertxOptions, DeploymentOptions deploymentOptions, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        Vertx.clusteredVertx(vertxOptions, vertxAsyncResult -> {
            Vertx vertx = vertxAsyncResult.result();

            EventBus eventBus = vertx.eventBus();

            //registers a handler to assert that all configurations are the same
            eventBus.consumer(NODE_COMMUNICATION_TOPIC).handler(message -> {
                try {
                    JsonObject jsonReply = new JsonObject().put("status", deploymentOptions.getConfig().equals(message.body()));
                    message.reply(jsonReply);
                } catch (Exception e) {
                    message.reply(new JsonObject().put("status", "invalid"));
                    log.error("Problem occurred parsing configuration and verifying configuration for clustering", e);
                }
            });

            vertx.deployVerticle(clazz(), deploymentOptions, handler -> {
                if (handler.failed()) {
                    log.error("Unable to ai.konduit.serving.deploy verticle {}", className(), handler.cause());
                    if(eventHandler != null) {
                        eventHandler.handle(Future.failedFuture(handler.cause()));
                    }
                } else {
                    try {
                        eventBus.request(NODE_COMMUNICATION_TOPIC, deploymentOptions.getConfig(), replyHandler -> {
                            if (replyHandler.failed()) {
                                log.error("Unable to get message reply", replyHandler.cause());
                            } else {
                                JsonObject reply = (JsonObject) replyHandler.result().body();
                                if (!reply.getBoolean("status")) {
                                    throw new IllegalStateException("Configuration not valid for cluster!");
                                } else
                                    log.info("Received cluster reply with {}", replyHandler.result().toString());
                            }
                        });
                    } catch (Exception e) {
                        log.error("Unable to parse json from configuration", e);
                        if(eventHandler != null) {
                            eventHandler.handle(Future.failedFuture(handler.cause()));
                        }
                    }

                    log.info("Deployed verticle {}", className());
                    if(eventHandler != null) {
                        VertxImpl vertxImpl = (VertxImpl) vertx;
                        DeploymentOptions inferenceDeploymentOptions = vertxImpl.getDeployment(handler.result()).deploymentOptions();

                        InferenceConfiguration inferenceConfiguration = ObjectMappers.fromJson(inferenceDeploymentOptions.getConfig().encode(), InferenceConfiguration.class);
                        eventHandler.handle(Future.succeededFuture(inferenceConfiguration));
                    }
                }
            });
        });
    }

    private static Class<? extends Verticle> clazz() {
        return ClusteredInferenceVerticle.class;
    }

    private static String className() {
        return clazz().getCanonicalName();
    }
}
