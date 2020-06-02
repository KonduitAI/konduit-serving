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

package ai.konduit.serving.metrics.prometheus;

import ai.konduit.serving.metrics.prometheus.test.MetricsTestingPipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.InferenceDeploymentResult;
import ai.konduit.serving.vertx.config.ServerProtocol;
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

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class TestMetricsEndpoint {

    public static final String PREDICT_ENDPOINT = "/predict";
    public static final String METRICS_ENDPOINT = "/metrics";

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
                        .add(new MetricsTestingPipelineStep())
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

    @Before
    public void before(){
        causeFailure.set(false);
    }

    @Test
    public void testMetricsEndpoint(TestContext testContext) throws Exception {
        Data input = JData.singleton("key", "value");


        for( int i=0; i<10; i++ ) {

            Response inference = given().port(inferenceDeploymentResult.getActualPort())
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(input.toJson())
                    .post(PREDICT_ENDPOINT)
                    .andReturn();

            Thread.sleep(10);

            Response metrics = given().port(inferenceDeploymentResult.getActualPort())
                    .get(METRICS_ENDPOINT)
                    .andReturn();

            testContext.assertEquals(200, metrics.statusCode());
            testContext.assertTrue(metrics.contentType().contains(ContentType.TEXT.toString()));
            String s = metrics.asString();
            System.out.println("Metrics:\n" + s);

            //Check that "counter" is connect value (equal to i) and "gauge" is equal to i * 0.1
            double counter = -1;
            double gauge = -1;
            for(String str : s.split("\n")){
                if(str.startsWith("#"))
                    continue;
                if(str.contains("MetricsTestingPipelineStep") && str.contains("counter")){
                    String[] split = str.split(" ");
                    counter = Double.parseDouble(split[1]);
                } else if(str.contains("MetricsTestingPipelineStep") && str.contains("gauge")){
                    String[] split = str.split(" ");
                    gauge = Double.parseDouble(split[1]);
                }
            }

            assertEquals(i+1, counter, 0.0);
            assertEquals(0.1 * i, gauge, 1e-8);
        }
    }

    @AfterClass
    public static void tearDown(TestContext testContext) {
        vertx.close(testContext.asyncAssertSuccess());
    }


}
