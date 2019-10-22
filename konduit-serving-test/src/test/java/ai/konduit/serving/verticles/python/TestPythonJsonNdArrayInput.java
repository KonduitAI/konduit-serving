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
import ai.konduit.serving.config.*;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.PythonPipelineStep;
import ai.konduit.serving.util.ObjectMapperHolder;
import ai.konduit.serving.util.python.PythonVariables;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.verticles.numpy.tensorflow.BaseMultiNumpyVerticalTest;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class TestPythonJsonNdArrayInput extends BaseMultiNumpyVerticalTest {

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

        return req -> {
            //should be json body of classification
            req.bodyHandler(body -> {
                System.out.println(body.toJson());
                System.out.println("Finish body" + body);
            });

            req.exceptionHandler(exception -> context.fail(exception));
        };
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCode("first += 2")
                .pythonInput("first", PythonVariables.Type.NDARRAY.name())
                .pythonOutput("first", PythonVariables.Type.NDARRAY.name())
                  .returnAllInputs(false)
                .build();

        PythonPipelineStep pythonStepConfig = PythonPipelineStep.builder()
                .inputName("default")
                .inputColumnName("default", Arrays.asList("first"))
                .inputSchema("default", new SchemaType[]{SchemaType.NDArray})
                .outputColumnName("default", Arrays.asList("output"))
                .outputSchema("default", new SchemaType[]{SchemaType.NDArray})
                .pythonConfig("default",pythonConfig)
                .build();


        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .inputDataType(Input.DataType.NUMPY)
                .predictionType(Output.PredictionType.RAW)
                .build();


        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .pipelineStep(pythonStepConfig)
                .servingConfig(servingConfig)
                .build();


        return new JsonObject(inferenceConfiguration.toJson());
    }


    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) throws Exception {
        this.context = context;

        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("first", Nd4j.scalar(2.0).toString());
        requestSpecification.body(jsonObject.encode().getBytes());
        requestSpecification.header("Content-Type","application/json");
        String body = requestSpecification.when()
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/dictionary").then()
                .extract()
        .body().asString();
        JsonObject jsonObject1 = new JsonObject(body);
        String ndarraySerde = jsonObject1.getJsonObject("default").toString();
        NDArrayOutput nd = ObjectMapperHolder.getJsonMapper().readValue(ndarraySerde,NDArrayOutput.class);
        INDArray value = nd.getNdArray();
        assertEquals(4,value.getDouble(0),1e-1);

    }

}
