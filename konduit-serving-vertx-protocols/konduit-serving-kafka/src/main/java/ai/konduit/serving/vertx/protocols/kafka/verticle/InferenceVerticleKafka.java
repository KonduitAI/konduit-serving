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

package ai.konduit.serving.vertx.protocols.kafka.verticle;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.config.KafkaConfiguration;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ai.konduit.serving.pipeline.settings.KonduitSettings.*;

@Slf4j
public class InferenceVerticleKafka extends InferenceVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
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

                KafkaConfiguration kafkaConfiguration = inferenceConfiguration.kafkaConfiguration();

                Map<String, String> configConsumer = new HashMap<>();
                configConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", inferenceConfiguration.host(), port));
                configConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getKafkaConsumerKeyDeserializerClass(kafkaConfiguration != null ? kafkaConfiguration.consumerKeyDeserializerClass() : null));
                configConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getKafkaConsumerValueDeserializerClass(kafkaConfiguration != null ? kafkaConfiguration.consumerValueDeserializerClass() : null));
                configConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, getConsumerGroupId(kafkaConfiguration != null ? kafkaConfiguration.consumerGroupId() : null));
                configConsumer.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getConsumerAutoOffsetReset(kafkaConfiguration != null ? kafkaConfiguration.consumerAutoOffsetReset() : null));
                configConsumer.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, getConsumerAutoCommit(kafkaConfiguration != null ? kafkaConfiguration.consumerAutoCommit() : null));

                String producerValueSerializerClass = getKafkaProducerValueSerializerClass(kafkaConfiguration != null ? kafkaConfiguration.producerValueSerializerClass() : null);

                Map<String, String> configProducer = new HashMap<>();
                configProducer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", inferenceConfiguration.host(), port));
                configProducer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, getKafkaProducerKeySerializerClass(kafkaConfiguration != null ? kafkaConfiguration.producerKeySerializerClass() : null));
                configProducer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerValueSerializerClass);
                configProducer.put(ProducerConfig.ACKS_CONFIG, getProducerAcks(kafkaConfiguration != null ? kafkaConfiguration.producerAcks() : null));

                KafkaConsumer consumer = KafkaConsumer.create(vertx, configConsumer);
                KafkaProducer producer = KafkaProducer.create(vertx, configProducer);

                consumer.handler(
                        recordIn -> {
                            KafkaConsumerRecord castedRecordIn = (KafkaConsumerRecord) recordIn;
                            Object input = castedRecordIn.value();

                            log.debug("Processing input from topic: {} at {}. " +
                                            "Headers={}, Key={}, " +
                                            "Value={}, Partition={}, " +
                                            "Offset={}",
                                    castedRecordIn.topic(), Date.from(Instant.ofEpochMilli(castedRecordIn.timestamp())),
                                    castedRecordIn.headers(), castedRecordIn.key(),
                                    input, castedRecordIn.partition(),
                                    castedRecordIn.offset());

                            Data output;

                            if(input instanceof Buffer) {
                                output = pipelineExecutor.exec(Data.fromBytes(((Buffer) input).getBytes()));
                            } else if(input instanceof JsonObject) {
                                output = pipelineExecutor.exec(Data.fromJson(((JsonObject) input).encode()));
                            } else if(input instanceof String) {
                                output = pipelineExecutor.exec(Data.fromJson((String) input));
                            } else {
                                throw new IllegalStateException("No conversion format exist for input value class type: " + input.getClass().getCanonicalName());
                            }

                            String producerTopicName = getProducerTopicName(kafkaConfiguration != null ? kafkaConfiguration.producerTopicName() : null);

                            KafkaProducerRecord recordOut;
                            if(producerValueSerializerClass.equals(BufferSerializer.class.getCanonicalName())) {
                                recordOut = KafkaProducerRecord.create(producerTopicName, Buffer.buffer(output.asBytes()));
                            } else if(producerValueSerializerClass.equals(JsonObjectSerializer.class.getCanonicalName())) {
                                recordOut = KafkaProducerRecord.create(producerTopicName, new JsonObject(output.toJson()));
                            } else if(producerValueSerializerClass.equals(StringSerializer.class.getCanonicalName())) {
                                recordOut = KafkaProducerRecord.create(producerTopicName, output.toJson());
                            } else {
                                throw new IllegalStateException("No conversion format exist for output value class type: " + producerValueSerializerClass);
                            }

                            producer.send(recordOut, recordOutHandler -> {
                                AsyncResult<RecordMetadata> castedRecordOutHandler = (AsyncResult<RecordMetadata>) recordOutHandler;

                                if (castedRecordOutHandler.succeeded()) {
                                    log.debug("Sent output to topic: {} at {}. " +
                                                    "Headers={}, Key={}, " +
                                                    "Value={}, Partition={}, " +
                                                    "Offset={}",
                                            recordOut.topic(), Date.from(Instant.ofEpochMilli(recordOut.timestamp())),
                                            recordOut.headers(), recordOut.key(),
                                            recordOut.value(), recordOut.partition(),
                                            castedRecordOutHandler.result().getOffset());
                                } else {
                                    log.error("Failed to send output to topic: {} at {}. " +
                                                    "Headers={}, Key={}, " +
                                                    "Value={}, Partition={}",
                                            recordOut.topic(), Date.from(Instant.ofEpochMilli(recordOut.timestamp())),
                                            recordOut.headers(), recordOut.key(),
                                            recordOut.value(), recordOut.partition(),
                                            castedRecordOutHandler.cause());
                                }
                            });
                        }
                );

                String consumerTopicName = getConsumerTopicName(kafkaConfiguration != null ? kafkaConfiguration.consumerTopicName() : null);

                consumer.subscribe(consumerTopicName, subscribeHandler -> {
                    AsyncResult<Void> castedSubscribeHandler = (AsyncResult<Void>) subscribeHandler;

                    if (castedSubscribeHandler.succeeded()) {
                        log.info("Subscribed to topic: {}", consumerTopicName);

                        if(getStartHttpServerForKafka(kafkaConfiguration != null ? kafkaConfiguration.startHttpServerForKafka() : Constants.DEFAULT_START_HTTP_SERVER_FOR_KAFKA)) {
                            String httpHost = getHttpKafkaHost(kafkaConfiguration != null ? kafkaConfiguration.httpKafkaHost() : Constants.DEFAULT_HTTP_KAFKA_HOST);
                            int httpPort = getHttpKafkaPort(kafkaConfiguration != null ? kafkaConfiguration. httpKafkaPort(): Constants.DEFAULT_HTTP_KAFKA_PORT);

                            log.info("Starting HTTP server for kafka on host {} and port {}", httpHost, httpPort);

                            vertx.createHttpServer(new HttpServerOptions()
                                    .setPort(httpPort)
                                    .setHost(httpHost)
                                    .setSsl(false)
                                    .setSslHandshakeTimeout(0)
                                    .setCompressionSupported(true)
                                    .setTcpKeepAlive(true)
                                    .setTcpNoDelay(true)
                                    .setAlpnVersions(Arrays.asList(HttpVersion.HTTP_1_0,HttpVersion.HTTP_1_1))
                                    .setUseAlpn(false))
                            .requestHandler(httpHandler -> {
                                if (httpHandler.path().equals("/health")) {
                                    httpHandler.response().end("Kafka server running");
                                } else {
                                    httpHandler.response().setStatusCode(404).end("Route not implemented");
                                }
                            })
                                    .exceptionHandler(throwable -> log.error("Error occurred during http request.", throwable))
                                    .listen(httpPort, httpHost, handler -> {
                                        if (handler.failed()) {
                                            startPromise.fail(handler.cause());
                                        } else {
                                            int actualPort = handler.result().actualPort();

                                            if(inferenceConfiguration.kafkaConfiguration() == null) {
                                                inferenceConfiguration.kafkaConfiguration(new KafkaConfiguration());
                                            }

                                            inferenceConfiguration.kafkaConfiguration().httpKafkaPort(actualPort);

                                            try {
                                                ((ContextInternal) context).getDeployment()
                                                        .deploymentOptions()
                                                        .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                                                long pid = getPid();

                                                saveInspectionDataIfRequired(pid);

                                                log.info("HTTP server for kafka is listening on host: '{}'", inferenceConfiguration.host());
                                                log.info("HTTP server for kafka started on port {}", actualPort);

                                                startPromise.complete();
                                            } catch (Throwable throwable) {
                                                startPromise.fail(throwable);
                                            }
                                        }
                                    });
                        } else {
                            long pid = getPid();

                            saveInspectionDataIfRequired(pid);

                            startPromise.complete();
                        }
                    } else {
                        log.error("Could not subscribe to topic: {}", consumerTopicName, castedSubscribeHandler.cause());
                        startPromise.fail(castedSubscribeHandler.cause());
                    }
                });
            }
        });
    }
}
