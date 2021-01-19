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

package ai.konduit.serving.vertx.protocols.mqtt.verticle;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import com.google.common.base.Strings;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class InferenceVerticleMqtt extends InferenceVerticle {

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
            if (resultHandler.failed()) {
                if (resultHandler.cause() != null)
                    startPromise.fail(resultHandler.cause());
                else {
                    startPromise.fail("Failed to start. Unknown cause.");
                }
            } else {
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

                MqttServerOptions mqttServerOptions = new MqttServerOptions()
                        .setHost(inferenceConfiguration.host())
                        .setPort(port);

                boolean useSsl = inferenceConfiguration.useSsl();
                String sslKeyPath = inferenceConfiguration.sslKeyPath();
                String sslCertificatePath = inferenceConfiguration.sslCertificatePath();

                if (useSsl) {
                    if (Strings.isNullOrEmpty(sslKeyPath) || Strings.isNullOrEmpty(sslCertificatePath)) {
                        if (Strings.isNullOrEmpty(sslKeyPath)) {
                            log.warn("No pem key file specified for SSL.");
                        }

                        if (Strings.isNullOrEmpty(sslCertificatePath)) {
                            log.warn("No pem certificate file specified for SSL.");
                        }

                        log.info("Using an auto generated self signed pem key and certificate with SSL.");
                        mqttServerOptions.setKeyCertOptions(SelfSignedCertificate.create().keyCertOptions());
                    } else {
                        sslKeyPath = new File(sslKeyPath).getAbsolutePath();
                        sslCertificatePath = new File(sslCertificatePath).getAbsolutePath();
                        log.info("Using SSL with PEM Key: {} and certificate {}.", sslKeyPath, sslCertificatePath);

                        mqttServerOptions.setPemKeyCertOptions(new PemKeyCertOptions().setKeyPath(sslKeyPath).setCertPath(sslCertificatePath));
                    }
                }

                MqttServer mqttServer = MqttServer.create(vertx, mqttServerOptions);
                mqttServer.endpointHandler(
                        endpoint -> {
                            log.info("MQTT client [{}] request to connect, clean session = {}",
                                    endpoint.clientIdentifier() ,
                                    endpoint.isCleanSession());

                            if (endpoint.auth() != null) {
                                log.info("[username = {}, password = {}]",
                                        endpoint.auth().getUsername(),
                                        endpoint.auth().getPassword());
                            }
                            if (endpoint.will() != null) {
                                log.info("[will topic = {} msg = {} QoS = {} isRetain = {}]",
                                        endpoint.will().getWillTopic(),
                                        endpoint.will().getWillMessageBytes(),
                                        endpoint.will().getWillQos(),
                                        endpoint.will().isWillRetain());
                            }

                            log.info("[keep alive timeout = {}]",
                                    endpoint.keepAliveTimeSeconds());

                            endpoint.accept(false)

                                    .disconnectHandler(v -> log.info("Received disconnect from client"))

                                    .subscribeHandler(handler ->
                                            endpoint.subscribeAcknowledge(handler.messageId(),
                                                    handler.topicSubscriptions().stream().map(subscription -> {
                                                        log.info("Subscription for {} with QoS {}",
                                                                subscription.topicName(),
                                                                subscription.qualityOfService());

                                                        return subscription.qualityOfService();
                                                    }).collect(Collectors.toList())))
                                    .unsubscribeHandler(handler -> {
                                        handler.topics().forEach(topic -> log.info("Unsubscription for {}", topic));
                                        endpoint.unsubscribeAcknowledge(handler.messageId());
                                    })

                                    .publishHandler(message -> {
                                        String messageString = message.payload().toString(StandardCharsets.UTF_8);
                                        log.info("Just received message [{}] with QoS [{}}]",
                                                messageString,
                                                message.qosLevel());

                                        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                                            endpoint.publishAcknowledge(message.messageId());
                                        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                                            endpoint.publishReceived(message.messageId());
                                        }

                                        endpoint.publish(message.topicName() + "-out",
                                                Buffer.buffer(pipelineExecutor.exec(Data.fromJson(messageString)).toJson()),
                                                MqttQoS.EXACTLY_ONCE,
                                                false,
                                                false);
                                    })

                                    .publishReleaseHandler(endpoint::publishComplete)

                                    .publishAcknowledgeHandler(messageId -> log.info("Received ack for message = {}", messageId))

                                    .publishReceivedHandler(endpoint::publishRelease)

                                    .publishCompletionHandler(messageId -> log.info("Received ack for message = {}", messageId))

                            .pingHandler(v -> log.info("Ping received from client"));
                        })
                        .listen(handler -> {
                            if (handler.failed()) {
                                startPromise.fail(handler.cause());
                            } else {
                                int actualPort = handler.result().actualPort();

                                inferenceConfiguration.port(actualPort);

                                try {
                                    ((ContextInternal) context).getDeployment()
                                            .deploymentOptions()
                                            .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                                    long pid = getPid();

                                    saveInspectionDataIfRequired(pid);

                                    log.info("MQTT server listening on host: '{}'", inferenceConfiguration.host());
                                    log.info("MQTT server started on port {}", actualPort);

                                    startPromise.complete();
                                } catch (Throwable throwable) {
                                    startPromise.fail(throwable);
                                }
                            }
                        });
            }
        });
    }
}
