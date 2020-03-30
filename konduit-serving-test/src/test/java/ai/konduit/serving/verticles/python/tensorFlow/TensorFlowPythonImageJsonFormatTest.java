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

package ai.konduit.serving.verticles.python.tensorFlow;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathUtils;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.PythonStep;
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
import org.datavec.python.PythonType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class TensorFlowPythonImageJsonFormatTest extends BaseMultiNumpyVerticalTest {

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
        String pythonCodePath = new ClassPathResource("scripts/tensorFlow/TensorFlowImageTest.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCodePath(pythonCodePath)
                .pythonPath(PythonPathUtils.getPythonPath())
                .pythonInput("img", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("prediction", PythonType.TypeName.NDARRAY.name())
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        //ServingConfig set httpport and Input Formats
        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.JSON)
                .httpPort(port).
                        build();

        //Model config and set model type as TensorFlow
        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .inputName("img")
                .dimensionsConfig("default", new Long[]{240L, 320L, 3L}) // Height, width, channels
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .steps(Arrays.asList(imageLoadingStep, pythonStepConfig))
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode());
        requestSpecification.header("Content-Type", "multipart/form-data");

        File imageFile = new ClassPathResource("data/TensorFlowImageTest.png").getFile();
        String output = requestSpecification.when()
                .multiPart("img", imageFile)
                .expect().statusCode(200)
                .post("/raw/image").then()
                .extract()
                .body().asString();

        JsonObject jsonObject1 = new JsonObject(output);
        String ndarraySerde = jsonObject1.getJsonObject("default").toString();
        NDArrayOutput nd = ObjectMappers.json().readValue(ndarraySerde, NDArrayOutput.class);
        INDArray outputArray = nd.getNdArray();
        assertEquals(7, outputArray.getDouble(0), 1e-1);

    }

    @Test(timeout = 60000)
    public void testInferenceClassificationResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode());
        requestSpecification.header("Content-Type", "multipart/form-data");

        File imageFile = new ClassPathResource("data/TensorFlowImageTest.png").getFile();
        String output = requestSpecification.when()
                .multiPart("img", imageFile)
                .expect().statusCode(200)
                .post("/classification/image").then()
                .extract()
                .body().asString();

        JsonObject jsonObject1 = new JsonObject(output);
        JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
        JsonArray probabilities = ndarraySerde.getJsonArray("probabilities");
        double outpuValue = probabilities.getJsonArray(0).getDouble(0);
        assertEquals(7, outpuValue, 1e-1);
    }
}
