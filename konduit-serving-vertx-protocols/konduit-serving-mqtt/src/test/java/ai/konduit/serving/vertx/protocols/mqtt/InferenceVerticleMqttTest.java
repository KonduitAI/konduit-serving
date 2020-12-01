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

package ai.konduit.serving.vertx.protocols.mqtt;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

@Slf4j
@RunWith(VertxUnitRunner.class)
public class InferenceVerticleMqttTest {
    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;

    static final String mqttHostName = "localhost";
    static final int mqttPort = 1883;
    static final int qos = 2;

    static final String publishTopicName = "inference";
    static final String subscribeTopicName = "inference-out";

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = new InferenceConfiguration()
                .protocol(ServerProtocol.MQTT)
                .host(mqttHostName)
                .port(mqttPort)
                .pipeline(SequencePipeline.builder()
                        .add(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO))
                        .build());

        Async async = testContext.async();

        vertx = DeployKonduitServing.deploy(new VertxOptions(),
                new DeploymentOptions(),
                configuration,
                handler -> {
                    if(handler.succeeded()) {
                        inferenceDeploymentResult = handler.result();
                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    @Test
    public void testMqttServer(TestContext testContext) {
        int countDown = 10;
        Async async = testContext.async(countDown);

        Data data = Data.empty();
        data.put("mqttMessageKey", "mqttMessageValue");

        MqttClient client = MqttClient.create(vertx);

        client.connect(mqttPort, mqttHostName, connectHandler -> {
            client.publishHandler(
                    publishHandler -> {
                        log.info("There are new message in topic: " + publishHandler.topicName());
                        log.info("Content(as string) of the message: " + publishHandler.payload().toString());
                        log.info("QoS: " + publishHandler.qosLevel());

                        testContext.assertEquals(publishHandler.topicName(), subscribeTopicName);
                        testContext.assertEquals(publishHandler.payload().toString(), data.toJson());
                        testContext.assertEquals(publishHandler.qosLevel().value(), qos);

                        async.countDown();
                    })
                    .subscribe(subscribeTopicName, qos);

            for (int i = 0; i < countDown; i++) {
                client.publish(publishTopicName,
                        Buffer.buffer(data.toJson()),
                        MqttQoS.EXACTLY_ONCE,
                        false,
                        false);
            }
        });
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
