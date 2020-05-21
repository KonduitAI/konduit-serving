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
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.ServerProtocol;
import ai.konduit.serving.vertx.protocols.grpc.api.InferenceGrpc;
import ai.konduit.serving.vertx.protocols.grpc.test.FailureTestingPipelineStep;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(VertxUnitRunner.class)
public class InferenceVerticleGrpcTest {

    static InferenceConfiguration configuration;
    static Vertx vertx;
    static ManagedChannel channel;
    static InferenceGrpc.InferenceVertxStub inferenceVertxStub;
    public static AtomicBoolean causeFailure = new AtomicBoolean();

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = InferenceConfiguration.builder()
                .protocol(ServerProtocol.GRPC)
                .pipeline(SequencePipeline.builder()
                        .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.ERROR).build())
                        .add(new FailureTestingPipelineStep())
                        .build())
                .build();

        Async async = testContext.async();

        vertx = DeployKonduitServing.deploy(new VertxOptions(),
                new DeploymentOptions(),
                configuration,
                handler -> {
                    if(handler.succeeded()) {
                        channel = VertxChannelBuilder
                                .forAddress(vertx, "localhost", handler.result().getActualPort())
                                .usePlaintext(true)
                                .build();

                        // Get a stub to use for interacting with the inference service
                        inferenceVertxStub = InferenceGrpc.newVertxStub(channel);

                        async.complete();
                    } else {
                        testContext.fail(handler.cause());
                    }
                });
    }

    @Before
    public void before(){
        causeFailure.set(false);
    }

    @Test
    public void testGrpcServerPass(TestContext testContext) throws InvalidProtocolBufferException {
        causeFailure.set(false);

        Data input = JData.singleton("key", "value");
        DataScheme request = DataScheme.parseFrom(input.asBytes());

        Async async = testContext.async();

        // Call the remote service
        inferenceVertxStub.predict(request, ar -> {
            if (ar.succeeded()) {
                testContext.assertEquals(input, Data.fromBytes(ar.result().toByteArray()));
                async.complete();
            } else {
                testContext.fail(ar.cause());
            }
        });
    }

    @Test
    public void testGrpcServerFail(TestContext testContext) throws InvalidProtocolBufferException {
        causeFailure.set(true);

        Data input = JData.singleton("key", "value");
        DataScheme request = DataScheme.parseFrom(input.asBytes());

        Async async = testContext.async();

        // Call the remote service
        inferenceVertxStub.predict(request, ar -> {
            if (ar.succeeded()) {
                testContext.fail("This should fail due to 'causeFailure' being 'true'");
            } else {
                async.complete();
            }
        });
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        channel.shutdownNow();
        vertx.close(testContext.asyncAssertSuccess());
    }


}
