/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.verticles.python;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.verticles.numpy.tensorflow.BaseMultiNumpyVerticalTest;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.concurrent.NotThreadSafe;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class TestPythonJsonInput extends BaseMultiNumpyVerticalTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {

        return req ->
            //should be json body of classification
            req.bodyHandler(body -> {
                System.out.println(body.toJson());
                System.out.println("Finish body" + body);
            }).exceptionHandler(exception -> context.fail(exception));
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCode("first += 2; second = first")
                .pythonInput("first", "INT")
                .pythonOutput("second", "INT")
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .inputDataFormat(Input.DataFormat.NUMPY)
                .predictionType(PredictionType.RAW)
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .step(pythonStepConfig)
                .servingConfig(servingConfig)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) {
        this.context = context;

        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject inputJson = new JsonObject();
        inputJson.put("first", 2);
        requestSpecification.body(inputJson.encode().getBytes());
        requestSpecification.header("Content-Type", "application/json");
        String responseBody = requestSpecification.when()
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/dictionary").then()
                .extract()
                .body().asString();
        JsonArray outputJsonArray = new JsonArray(responseBody);
        JsonObject result = outputJsonArray.getJsonObject(0);
        assertTrue(result.containsKey("second"));
        assertEquals(4, result.getInteger("second"), 1e-1);
    }
}
