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

package ai.konduit.serving.verticles.python.scikitlearn;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.ExpectedAssertUtil;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
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
import org.apache.commons.io.FileUtils;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
@Ignore
public class ScikitLearnPythonImageNd4jFormatTest extends BaseMultiNumpyVerticalTest {
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

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

        String pythonCodePath = new ClassPathResource("scripts/scikitlearn/Image_Scikitlearn_NDarray.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonCodePath(pythonCodePath)
                .pythonInput("imgPath", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("result", PythonType.TypeName.NDARRAY.name())
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.ND4J)
                .httpPort(port)
                .build();


        //Model config and set model type as ScikitLearn
        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .inputName("imgPath")
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

        File imageFile = new ClassPathResource("data/ScikitlearnImageTest.png").getFile();
        String output = requestSpecification.when()
                .multiPart("imgPath", imageFile)
                .expect().statusCode(200)
                .post("/raw/image").then()
                .extract()
                .body().asString();

        File outputImagePath = new File(testDir.newFolder(), "file.json");
        FileUtils.writeStringToFile(outputImagePath, output, Charset.defaultCharset());
        INDArray outputArray = BinarySerde.readFromDisk(outputImagePath);
        INDArray expectedArr = ExpectedAssertUtil.NdArrayAssert("src/test/resources/Json/scikitlearn/ScikitlearnImageTest.json", "raw");
        assertEquals(expectedArr, outputArray);
    }

    @Test(timeout = 60000)
    @Ignore
    public void testInferenceClassificationResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode());
        requestSpecification.header("Content-Type", "multipart/form-data");

        File imageFile = new ClassPathResource("data/ScikitlearnImageTest.png").getFile();
        String output = requestSpecification.when()
                .multiPart("imgPath", imageFile)
                .expect().statusCode(200)
                .post("/classification/image").then()
                .extract()
                .body().asString();
        JsonObject jsonObject1 = new JsonObject(output);
        assertTrue(jsonObject1.containsKey("default"));
        assertTrue(jsonObject1.getJsonObject("default").containsKey("probabilities"));
        JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
        JsonArray outputArr = ndarraySerde.getJsonArray("probabilities");
        double outpuValue = outputArr.getJsonArray(0).getDouble(0);
        JsonArray expArr = ExpectedAssertUtil.ProbabilitiesAssert("src/test/resources/Json/scikitlearn/ScikitlearnImageTest.json");
        double expValue = expArr.getJsonArray(0).getDouble(0);
        assertEquals(expValue, outpuValue, 1e-1);
        assertEquals(expArr, outputArr);
    }

}
