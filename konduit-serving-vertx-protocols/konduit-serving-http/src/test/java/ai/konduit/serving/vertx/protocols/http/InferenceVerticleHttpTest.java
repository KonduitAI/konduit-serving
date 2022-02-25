/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
import ai.konduit.serving.vertx.protocols.http.api.ErrorResponse;
import ai.konduit.serving.vertx.protocols.http.test.FailureTestingPipelineStep;
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

import java.util.concurrent.atomic.AtomicBoolean;

import static ai.konduit.serving.vertx.protocols.http.api.HttpApiErrorCode.*;
import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class InferenceVerticleHttpTest {

    public static final String PREDICT_ENDPOINT = "/predict";

    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;
    public static AtomicBoolean causeFailure = new AtomicBoolean();

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = new InferenceConfiguration()
                .protocol(ServerProtocol.HTTP)
                .pipeline(SequencePipeline.builder()
                        .add(new LoggingStep().log(LoggingStep.Log.KEYS_AND_VALUES).logLevel(Level.ERROR))
                        .add(new FailureTestingPipelineStep())
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

    @Before
    public void before(){
        causeFailure.set(false);
    }

    @Test
    public void inferenceVerticleHttpTestJsonToJson(TestContext testContext) {
        Data input = JData.singleton("key_json_to_json", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(input.toJson())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
        testContext.assertEquals(input, Data.fromJson(response.asString()));
    }

    @Test
    public void inferenceVerticleHttpTestJsonToBinary(TestContext testContext) {
        Data input = JData.singleton("key_json_to_binary", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.BINARY)
                .body(input.toJson())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.BINARY.toString(), response.contentType());
        testContext.assertEquals(input, Data.fromBytes(response.asByteArray()));
    }

    @Test
    public void inferenceVerticleHttpTestBinaryToJson(TestContext testContext) {
        Data input = JData.singleton("key_binary_to_json", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept(ContentType.JSON)
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
        testContext.assertEquals(input, Data.fromJson(response.asString()));
    }

    @Test
    public void inferenceVerticleHttpTestBinaryToBinary(TestContext testContext) {
        Data input = JData.singleton("key_binary_to_binary", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept(ContentType.BINARY)
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
        testContext.assertEquals(ContentType.BINARY.toString(), response.contentType());
        testContext.assertEquals(input, Data.fromBytes(response.asByteArray()));
    }

    @Test
    public void testEmptyOrNullContentTypeHeader(TestContext testContext) {
        Data input = JData.singleton("key_null_or_empty_content_type_header", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType("")
                .accept(ContentType.BINARY)
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        if(response.statusCode() != 415) {
            testContext.assertEquals(500, response.statusCode());
            testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
            testContext.assertEquals(MISSING_OR_EMPTY_CONTENT_TYPE_HEADER.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
        }
    }

    @Test
    public void testEmptyOrNullAcceptHeader(TestContext testContext) {
        Data input = JData.singleton("key_null_or_empty_accept_header", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept("") // empty
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(500, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
        testContext.assertEquals(MISSING_OR_EMPTY_ACCEPT_HEADER.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
    }

    @Test
    public void testInvalidContentTypeHeader(TestContext testContext) {
        Data input = JData.singleton("invalid_content_type_header", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.TEXT) // invalid
                .accept(ContentType.BINARY)
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        if(response.statusCode() != 415) {
            testContext.assertEquals(500, response.statusCode());
            testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
            testContext.assertEquals(INVALID_CONTENT_TYPE_HEADER.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
        }
    }

    @Test
    public void testInvalidAcceptHeader(TestContext testContext) {
        Data input = JData.singleton("invalid_accept_header", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept(ContentType.TEXT) // invalid
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        if(response.statusCode() != 406) {
            testContext.assertEquals(500, response.statusCode());
            testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
            testContext.assertEquals(INVALID_ACCEPT_HEADER.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
        }
    }

    @Test
    public void testInvalidData(TestContext testContext) {
        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept(ContentType.BINARY)
                .body(new byte[] {0x11, 0x22, 0x33, 0x44}) // invalid data
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(500, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
        testContext.assertEquals(DATA_PARSING_ERROR.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
    }

    @Test
    public void testFailedPipeline(TestContext testContext) {
        causeFailure.set(true);
        Data input = JData.singleton("invalid_accept_header", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.BINARY)
                .accept(ContentType.BINARY)
                .body(input.asBytes())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(500, response.statusCode());
        testContext.assertEquals(ContentType.JSON.toString(), response.contentType());
        testContext.assertEquals(PIPELINE_PROCESSING_ERROR.name(), ErrorResponse.fromJson(response.asString()).getErrorCode().name());
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }


}
