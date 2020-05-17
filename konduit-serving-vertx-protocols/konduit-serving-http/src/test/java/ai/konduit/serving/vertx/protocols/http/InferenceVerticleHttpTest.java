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
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class InferenceVerticleHttpTest {

    public static final String PREDICT_ENDPOINT = "/predict";

    static InferenceConfiguration configuration;
    static Vertx vertx;
    static InferenceDeploymentResult inferenceDeploymentResult;

    @BeforeClass
    public static void setUp(TestContext testContext) {
        configuration = InferenceConfiguration.builder()
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
    public void inferenceVerticleHttpTestJsonToJson(TestContext testContext) {
        Data input = JData.singleton("key_json_to_json", false);

        Response response = given().port(inferenceDeploymentResult.getActualPort())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(input.toJson())
                .post(PREDICT_ENDPOINT)
                .andReturn();

        testContext.assertEquals(200, response.statusCode());
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
        testContext.assertEquals(input, Data.fromBytes(response.asByteArray()));
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }
}
