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

package ai.konduit.serving.verticles.python.pytorch;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.miscutils.ExpectedAssertUtil;
import ai.konduit.serving.util.ObjectMappers;
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
import org.apache.commons.io.FileUtils;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class PytorchPythonNumpyJsonFormatTest extends BaseMultiNumpyVerticalTest {

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
        String pythonCodePath = new ClassPathResource("scripts/pytorch/NumpyPytorchNumpy.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonCodePath(pythonCodePath)
                .pythonInput("inputValue", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("pred", PythonType.TypeName.NDARRAY.name())
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

    @Test
    public void testInferenceResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        //Preparing input NDArray
        INDArray arr = Nd4j.create(new float[][]{{1, 0, 5, 10}, {100, 55, 555, 1000}});

        byte[] xNpy = Nd4j.toNpyByteArray(arr);

        File xFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(xFile, xNpy);

        requestSpecification.header("Content-Type", "multipart/form-data");
        String response = requestSpecification.when()
                .multiPart("default", xFile)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/numpy").then()
                .extract()
                .body().asString();

        JsonObject jsonObject = new JsonObject(response);
        assertTrue(jsonObject.containsKey("default"));
        assertTrue(jsonObject.getJsonObject("default").containsKey("ndArray"));
        assertTrue(jsonObject.getJsonObject("default").getJsonObject("ndArray").containsKey("data"));
        String ndarraySerde = jsonObject.getJsonObject("default").toString();
        NDArrayOutput nd = ObjectMappers.json().readValue(ndarraySerde, NDArrayOutput.class);
        INDArray outputArray = nd.getNdArray();
        INDArray expectedArr = ExpectedAssertUtil.NdArrayAssert("src/test/resources/Json/pytorch/PytorchNumpyTest.json", "raw");
        assertEquals(expectedArr, outputArray);
    }


    @Test
    @Ignore
    public void testInferenceClassificationResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();

        //Preparing input NDArray
        INDArray arr = Nd4j.create(new float[][]{{1, 0, 5, 10}, {100, 55, 555, 1000}});

        byte[] xNpy = Nd4j.toNpyByteArray(arr);

        File xFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(xFile, xNpy);

        requestSpecification.header("Content-Type", "multipart/form-data");
        String response = requestSpecification.when()
                .multiPart("default", xFile)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/classification/numpy").then()
                .extract()
                .body().asString();

        JsonObject jsonObjectClassification = new JsonObject(response);
        assertTrue(jsonObjectClassification.containsKey("default"));
        assertTrue(jsonObjectClassification.getJsonObject("default").containsKey("probabilities"));
        JsonObject ndarraySerde = jsonObjectClassification.getJsonObject("default");
        JsonArray probabilities = ndarraySerde.getJsonArray("probabilities");
        float[][] nd = ObjectMappers.json().readValue(probabilities.toString(), float[][].class);
        INDArray outputArray = Nd4j.create(nd);
        JsonArray expArr = ExpectedAssertUtil.ProbabilitiesAssert("src/test/resources/Json/pytorch/PytorchNumpyTest.json");
        float[][] expNd = ObjectMappers.json().readValue(expArr.toString(), float[][].class);
        INDArray expectedArray = Nd4j.create(expNd);
        assertEquals(expectedArray, outputArray);
    }

}
