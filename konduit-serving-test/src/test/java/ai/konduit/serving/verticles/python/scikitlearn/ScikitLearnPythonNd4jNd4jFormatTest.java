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
import ai.konduit.serving.miscutils.PythonPathUtils;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.verticles.numpy.tensorflow.BaseMultiNumpyVerticalTest;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.transform.ImageTransformProcess;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.ByteBuffer;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class ScikitLearnPythonNd4jNd4jFormatTest extends BaseMultiNumpyVerticalTest {

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

        String pythonCodePath = new ClassPathResource("scripts/scikitlearn/NDArrayScikitNDArrayInf.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(PythonPathUtils.getPythonPath())
                .pythonCodePath(pythonCodePath)
                .pythonInput("imgPath", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("Ypredict", PythonType.TypeName.NDARRAY.name())
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.ND4J)
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

        ImageTransformProcess imageTransformProcess = new ImageTransformProcess.Builder()
                .scaleImageTransform(20.0f)
                .resizeImageTransform(28, 28)
                .build();

        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .inputName("default")
                .dimensionsConfig("default", new Long[]{28L, 28L, 1L}) // Height, width, channels
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        String imagePath = new ClassPathResource("data/ScikitLearnNDArray.png").getFile().getAbsolutePath();

        Writable[][] output = imageLoadingStep.createRunner().transform(imagePath);

        INDArray image = ((NDArrayWritable) output[0][0]).get();
        File file = new File(testDir.newFolder(), "file.json");
        BinarySerde.writeArrayToDisk(image.reshape(28, 28), file);
        requestSpecification.body(jsonObject.encode().getBytes());

        requestSpecification.header("Content-Type", "multipart/form-data");
        Response response = requestSpecification.when()
                .multiPart("default", file)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/nd4j")
                .andReturn();

        INDArray outputArray = BinarySerde.toArray(ByteBuffer.wrap(response.getBody().asByteArray())).castTo(DataType.INT32);
        INDArray expectedArr = ExpectedAssertUtil.fileAndKeyToNDArrayOutput("src/test/resources/Json/scikitlearn/ScikitlearnNdArrayTest.json", "raw");
        assertEquals(expectedArr, outputArray);
    }

    @Test(timeout = 60000)
    @Ignore
    public void testInferenceClassificationResult(TestContext context) throws Exception {
        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();

        ImageTransformProcess imageTransformProcess = new ImageTransformProcess.Builder()
                .scaleImageTransform(20.0f)
                .resizeImageTransform(28, 28)
                .build();

        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .inputName("default")
                .dimensionsConfig("default", new Long[]{28L, 28L, 1L}) // Height, width, channels
                .imageTransformProcess("default", imageTransformProcess)
                .build();

        String imagePath = new ClassPathResource("data/ScikitLearnNDArray.png").getFile().getAbsolutePath();

        Writable[][] output = imageLoadingStep.createRunner().transform(imagePath);

        INDArray image = ((NDArrayWritable) output[0][0]).get();

        File file = new File(testDir.newFolder(), "file.json");
        BinarySerde.writeArrayToDisk(image.reshape(28, 28), file);
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
        JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
        JsonArray outputArr = ndarraySerde.getJsonArray("probabilities");
        double outpuValue = outputArr.getJsonArray(0).getDouble(0);
        JsonArray expArr = ExpectedAssertUtil.probabilitiesToJsonArray("src/test/resources/Json/scikitlearn/ScikitlearnNdArrayTest.json");
        double expValue = expArr.getJsonArray(0).getDouble(0);
        assertEquals(expValue, outpuValue, 1e-1);
        assertEquals(expArr, outputArr);

    }


}
