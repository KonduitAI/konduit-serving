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

package ai.konduit.serving.vertx.protocols.grpc;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import ai.konduit.serving.vertx.protocols.grpc.api.ApiGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.grpc.VertxChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(VertxUnitRunner.class)
public class InferenceVerticleGrpcTest {

    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;
    public static AtomicBoolean causeFailure = new AtomicBoolean();

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = InferenceConfiguration.builder()
                .protocol(ServerProtocol.GRPC)
                .pipeline(SequencePipeline.builder()
                        .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.ERROR).build())
                        .build())
                .build();

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
    public void testGrpcServer(TestContext testContext) throws InvalidProtocolBufferException {
        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, "localhost", inferenceDeploymentResult.getActualPort())
                .usePlaintext(true)
                .build();

        // Get a stub to use for interacting with the remote service
        ApiGrpc.ApiVertxStub stub = ApiGrpc.newVertxStub(channel);

        Data input = JData.singleton("key", "value");
        DataProtoMessage.DataScheme request = DataProtoMessage.DataScheme.parseFrom(input.asBytes());

        // Call the remote service
        stub.predict(request, ar -> {
            if (ar.succeeded()) {
                testContext.assertEquals(input, Data.fromBytes(ar.result().toByteArray()));
            } else {
                testContext.fail(ar.cause());
            }
        });
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }


}
