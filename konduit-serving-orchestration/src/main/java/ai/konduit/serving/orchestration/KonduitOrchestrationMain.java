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
import ai.konduit.serving.configprovider.KonduitServingNodeConfigurer;
import com.beust.jcommander.JCommander;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.nd4j.base.Preconditions;
import org.nd4j.shade.jackson.core.JsonProcessingException;

import java.io.IOException;

/**
 * Multi node/clustered setup using
 * {@link KonduitServingNodeConfigurer}
 * and {@link Vertx#clusteredVertx(VertxOptions, Handler) }
 * for initialization for multi node communication
 *
 * @author Adam Gibson
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KonduitOrchestrationMain {
    private Runnable onSuccess, onFailure;

    public final static String NODE_COMMUNICATION_TOPIC = "NodeCommunication";
    private static Logger log = LoggerFactory.getLogger(KonduitOrchestrationMain.class.getName());
    private EventBus eventBus;
    private KonduitServingNodeConfigurer configurer;

    public static void main(String... args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitOrchestrationMain().runMain(args);
            log.debug("Exiting model server.");
        } catch (Exception e) {
            log.error("Unable to start model server.", e);
            throw e;
        }
    }

    public void runMain(KonduitServingNodeConfigurer konduitServingNodeConfigurer) {
        //force clustering
        this.configurer = konduitServingNodeConfigurer;
        konduitServingNodeConfigurer.setClustered(true);

        konduitServingNodeConfigurer.setupVertxOptions();
        Vertx.clusteredVertx(konduitServingNodeConfigurer.getVertxOptions(), vertxAsyncResult -> {
            Vertx vertx = vertxAsyncResult.result();
            ConfigRetriever configRetriever = ConfigRetriever.create(vertx, konduitServingNodeConfigurer.getConfigRetrieverOptions());
            eventBus = vertx.eventBus();
            //registers a handler to assert that all configurations are the same
            registerHandler();

            configRetriever.getConfig(result -> {
                if (result.failed()) {
                    log.error("Unable to retrieve configuration " + result.cause());
                } else {
                    JsonObject result1 = result.result();
                    konduitServingNodeConfigurer.configureWithJson(result1);
                    konduitServingNodeConfigurer.setVerticleClassName(ClusteredInferenceVerticle.class.getName());


                    vertx.deployVerticle(konduitServingNodeConfigurer.getVerticleClassName(), konduitServingNodeConfigurer.getDeploymentOptions(), handler -> {
                        if (handler.failed()) {
                            log.error("Unable to deploy verticle {}", konduitServingNodeConfigurer.getVerticleClassName(), handler.cause());
                            if(onFailure != null) {
                                onFailure.run();
                            }
                        } else {
                            configRetriever.close(); // We don't need the config retriever to periodically scan for config after it is successfully retrieved.

                            try {
                                Preconditions.checkNotNull(konduitServingNodeConfigurer.getInferenceConfiguration(), "Node configurer inference configuration was null!");
                                eventBus.request(NODE_COMMUNICATION_TOPIC, new JsonObject(konduitServingNodeConfigurer.getInferenceConfiguration().toJson()), replyHandler -> {
                                    if (replyHandler.failed()) {
                                        log.error("Unable to get message reply", replyHandler.cause());
                                    } else {
                                        JsonObject reply = (JsonObject) replyHandler.result().body();
                                        if (!reply.getBoolean("status")) {
                                            throw new IllegalStateException("Configuration not valid for cluster!");
                                        } else
                                            log.info("Received cluster reply with ", replyHandler.result().toString());
                                    }
                                });
                            } catch (JsonProcessingException e) {
                                log.error("Unable to parse json from configuration", e);
                                if(onFailure != null) {
                                    onFailure.run();
                                }
                            }

                            log.info("Deployed verticle {}", konduitServingNodeConfigurer.getVerticleClassName());
                            if(onSuccess != null) {
                                onSuccess.run();
                            }
                        }
                    });

                }
            });
        });
    }


    private void registerHandler() {
        MessageConsumer<JsonObject> messageConsumer = eventBus.consumer(NODE_COMMUNICATION_TOPIC);
        messageConsumer.handler(message -> {
            JsonObject jsonMessage = message.body();
            try {
                InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(jsonMessage.toString());
                JsonObject jsonReply = new JsonObject().put("status", inferenceConfiguration.equals(configurer.getInferenceConfiguration()));
                message.reply(jsonReply);

            } catch (IOException e) {
                JsonObject jsonReply = new JsonObject().put("status", "invalid");
                message.reply(jsonReply);
                log.error("Problem occurred parsing configuration and verifying configuration for clustering", e);
            }

        });
    }


    public void runMain(String... args) {
        log.debug("Parsing args " + java.util.Arrays.toString(args));
        KonduitServingNodeConfigurer konduitServingNodeConfigurer = new KonduitServingNodeConfigurer();
        JCommander jCommander = new JCommander(konduitServingNodeConfigurer);
        jCommander.parse(args);
        runMain(konduitServingNodeConfigurer);
    }

}
