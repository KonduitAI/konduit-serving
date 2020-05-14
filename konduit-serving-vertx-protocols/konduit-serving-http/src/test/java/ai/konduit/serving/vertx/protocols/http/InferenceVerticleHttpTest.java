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
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import com.jayway.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;

import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class InferenceVerticleHttpTest {

    InferenceConfiguration configuration;

    @Before
    public void setUp() {
        configuration = InferenceConfiguration.builder()
                .pipeline(SequencePipeline.builder()
                        .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).build())
                        .build())
                .build();
    }

    @Test
    public void inferenceVerticleHttpTest(TestContext testContext) {
        Async async = testContext.async();

        DeployKonduitServing.deploy(new VertxOptions(), new DeploymentOptions(), configuration, handler -> {
            if(handler.succeeded()) {
                given().port(handler.result().getActualPort())
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body(new JsonObject()
                                .put("key1", false))
                        .post("/predict")
                        .then().assertThat()
                        .statusCode(200)
                        .and().assertThat()
                        .body(Matchers.not(Matchers.empty()));

                async.complete();
            } else {
                testContext.fail(handler.cause());
            }
        });
    }
}
