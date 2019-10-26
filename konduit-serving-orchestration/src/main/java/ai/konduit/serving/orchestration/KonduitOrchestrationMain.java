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

import ai.konduit.serving.configprovider.KonduitServingNodeConfigurer;
import ai.konduit.serving.InferenceConfiguration;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.VertxOptions;
import  com.beust.jcommander.JCommander;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.eventbus.EventBus;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.eventbus.MessageConsumer;
/**
 * Multi node/clustered setup using
 * {@link KonduitServingNodeConfigurer}
 * and {@link Vertx#clusteredVertx(VertxOptions, Handler) }
 * for initialization for multi node communication
 *
 * @author Adam Gibson
 */
public class KonduitOrchestrationMain {

    private static Logger log = LoggerFactory.getLogger(KonduitOrchestrationMain.class.getName());
    private EventBus eventBus;
    public final static String NODE_COMMUNICATION_TOPIC = "NodeCommunication";
    private KonduitServingNodeConfigurer configurer;

    public static void main(String...args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitOrchestrationMain().runMain(args);
            log.debug("Exiting model server.");
        }catch(Exception e) {
            log.error("Unable to start model server.",e);
            throw e;
        }
    }

    public void runMain(KonduitServingNodeConfigurer konduitServingNodeConfigurer) {
        //force clustering
        this.configurer = konduitServingNodeConfigurer;
        konduitServingNodeConfigurer.setClustered(true);

        konduitServingNodeConfigurer.setupVertxOptions();
        Vertx.clusteredVertx(konduitServingNodeConfigurer.getVertxOptions(),vertxAsyncResult -> {
            Vertx vertx = vertxAsyncResult.result();
            ConfigRetriever configRetriever = io.vertx.config.ConfigRetriever.create(vertx,konduitServingNodeConfigurer.getOptions());
            eventBus = vertx.eventBus();
            //registers a handler to assert that all configurations are the same
            registerHandler();
            configRetriever.getConfig(result -> {
                if(result.failed()) {
                    log.error("Unable to retrieve configuration " + result.cause());
                }
                else {
                    JsonObject result1 = result.result();
                    konduitServingNodeConfigurer.configureWithJson(result1);
                    try {
                        eventBus.send(NODE_COMMUNICATION_TOPIC,new io.vertx.core.json.JsonObject(konduitServingNodeConfigurer.getInferenceConfiguration().toJson()),replyHandler -> {
                            vertx.deployVerticle(konduitServingNodeConfigurer.getVerticleClassName(),konduitServingNodeConfigurer.getDeploymentOptions(),handler -> {
                                if(handler.failed()) {
                                    log.error("Unable to deploy verticle {}",konduitServingNodeConfigurer.getVerticleClassName(),handler.cause());
                                }
                                else {
                                    log.info("Deployed verticle {}",konduitServingNodeConfigurer.getVerticleClassName());
                                }
                            });
                        });
                    } catch (org.nd4j.shade.jackson.core.JsonProcessingException e) {
                        log.error("Unable to find")
                    }

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
                JsonObject jsonReply = new JsonObject().put("status",String.valueOf(inferenceConfiguration.equals(configurer.getInferenceConfiguration())));
                message.reply(jsonReply);

            } catch (java.io.IOException e) {
                JsonObject jsonReply = new JsonObject().put("status","invalid");
                message.reply(jsonReply);
                e.printStackTrace();
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
