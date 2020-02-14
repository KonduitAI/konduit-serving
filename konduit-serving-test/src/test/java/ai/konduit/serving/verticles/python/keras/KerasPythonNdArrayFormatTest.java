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

package ai.konduit.serving.verticles.python.keras;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.util.ExpectedAssertTest;
import ai.konduit.serving.util.ObjectMapperHolder;
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
import org.datavec.python.PythonVariables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class KerasPythonNdArrayFormatTest extends BaseMultiNumpyVerticalTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {

        return req -> {
            //should be json body of classification
            req.bodyHandler(body -> {
            });

            req.exceptionHandler(exception -> context.fail(exception));
        };
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        String pythonPath = Arrays.stream(cachePackages())
                .filter(Objects::nonNull)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
        String pythonCodePath = new ClassPathResource("scripts/keras/KerasNDArrayTest.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonCodePath(pythonCodePath)
                .pythonInput("inputData", PythonVariables.Type.NDARRAY.name())
                .pythonOutput("arr", PythonVariables.Type.NDARRAY.name())
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .step(pythonStepConfig)
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

        //Preparing input NDArray
        INDArray arr = Nd4j.create(new float[][]{{1, 0, 5, 10}, {100, 55, 555, 1000}});

        String filePath = new ClassPathResource("data").getFile().getAbsolutePath();

        //Create new file to write binary input data.
        File file = new File(filePath + "/test-input.zip");

        BinarySerde.writeArrayToDisk(arr, file);
        requestSpecification.body(jsonObject.encode().getBytes());

        requestSpecification.header("Content-Type", "multipart/form-data");
        String response = requestSpecification.when()
                .multiPart("default", file)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/nd4j").then()
                .extract()
                .body().asString();

        JsonObject jsonObject1 = new JsonObject(response);
        assertTrue(jsonObject1.containsKey("default"));
        assertTrue(jsonObject1.getJsonObject("default").containsKey("ndArray"));
        assertTrue(jsonObject1.getJsonObject("default").getJsonObject("ndArray").containsKey("data"));
        String ndarraySerde = jsonObject1.getJsonObject("default").toString();
        NDArrayOutput nd = ObjectMapperHolder.getJsonMapper().readValue(ndarraySerde, NDArrayOutput.class);
        INDArray outputArray = nd.getNdArray();
        INDArray expectedArr = ExpectedAssertTest.NdArrayAssert("src/test/resources/Json/keras/KerasNdArrayTest.json", "raw");
        assertEquals(expectedArr, outputArray);
    }

    @Test(timeout = 60000)
    public void testInferenceClassificationResult(TestContext context) throws Exception {
        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();

        //Preparing input NDArray
        INDArray arr = Nd4j.create(new float[][]{{1, 0, 5, 10}, {100, 55, 555, 1000}});

        String filePath = new ClassPathResource("data").getFile().getAbsolutePath();

        //Create new file to write binary input data.
        File file = new File(filePath + "/test-input.zip");

        BinarySerde.writeArrayToDisk(arr, file);
        requestSpecification.body(jsonObject.encode().getBytes());

        requestSpecification.header("Content-Type", "multipart/form-data");
        String response = requestSpecification.when()
                .multiPart("default", file)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/classification/nd4j").then()
                .extract()
                .body().asString();

        JsonObject jsonObject1 = new JsonObject(response);
        assertTrue(jsonObject1.containsKey("default"));
        assertTrue(jsonObject1.getJsonObject("default").containsKey("probabilities"));
        JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
        JsonArray probabilities = ndarraySerde.getJsonArray("probabilities");
        float[][] nd = ObjectMapperHolder.getJsonMapper().readValue(probabilities.toString(), float[][].class);
        INDArray outputArray = Nd4j.create(nd);
        JsonArray expProb = ExpectedAssertTest.ProbabilitiesAssert("src/test/resources/Json/keras/KerasNdArrayTest.json");
        float[][] expNd = ObjectMapperHolder.getJsonMapper().readValue(expProb.toString(), float[][].class);
        INDArray expectedArray = Nd4j.create(expNd);
        assertEquals(expectedArray, outputArray);
    }
}
