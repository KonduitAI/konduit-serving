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

package ai.konduit.serving.vertx.protocols.http;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import ai.konduit.serving.vertx.protocols.http.test.CustomGetEndpoint;
import ai.konduit.serving.vertx.protocols.http.test.CustomHttpEndpoint;
import ai.konduit.serving.vertx.protocols.http.test.CustomPostEndpoint;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class CustomEndpointTest {

    public static final String PREDICT_ENDPOINT = "/predict";

    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;
    public static AtomicBoolean causeFailure = new AtomicBoolean();

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = InferenceConfiguration.builder()
                .protocol(ServerProtocol.HTTP)
                .pipeline(SequencePipeline.builder()
                        .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.ERROR).build())
                        .build())
                .customEndpoints(Collections.singletonList(CustomHttpEndpoint.class.getName()))
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

    @Before
    public void before(){
        causeFailure.set(false);
    }

    @Test
    public void inferenceVerticleCustomPost(TestContext testContext) {
        Data input = JData.singletonList("someKey", Arrays.asList("x", "y", "z"), ValueType.STRING);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.TEXT)
                .body(input.toJson())
                .post(CustomPostEndpoint.PATH)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.TEXT.toString(), response.contentType());

        testContext.assertEquals(input, CustomPostEndpoint.input);
        testContext.assertEquals(CustomPostEndpoint.output, response.asString());
        //System.out.println(response.asString());

        //Test standard inference:
        response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(input.toJson())
                .post(InferenceVerticleHttpTest.PREDICT_ENDPOINT)
                .andReturn();

        String s = response.asString();
        Data d = Data.fromJson(s);
        assertEquals(input, d);
    }

    @Test
    public void inferenceVerticleCustomGet(TestContext testContext) {

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .accept(ContentType.JSON)
                .get(CustomGetEndpoint.PATH)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());

        String s = response.asString();
        Data d = Data.fromJson(s);

        testContext.assertEquals(d, CustomGetEndpoint.output);

        //Test standard inference:
        Data input = JData.singletonList("someKey", Arrays.asList("x", "y", "z"), ValueType.STRING);
        response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(input.toJson())
                .post(InferenceVerticleHttpTest.PREDICT_ENDPOINT)
                .andReturn();

        s = response.asString();
        d = Data.fromJson(s);
        assertEquals(input, d);
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }


}
