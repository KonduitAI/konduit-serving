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
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.ExpectedAssertUtil;
import ai.konduit.serving.miscutils.PythonPathInfo;
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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
@Ignore
public class PytorchPythonImageNumpyFormatTest extends BaseMultiNumpyVerticalTest {

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
        String pythonCodePath = new ClassPathResource("scripts/face_detection_pytorch/detect_image.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCodePath(pythonCodePath)
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonInput("image", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("num_boxes", PythonType.TypeName.NDARRAY.name())
                .build();

        PythonStep pythonStepConfig = new PythonStep(pythonConfig);

        //ServingConfig set httpport and Input Formats
        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.NUMPY)
                .httpPort(port).
                        build();

        //Model config and set model type as Pytorch
        ImageLoadingStep imageLoadingStep = ImageLoadingStep.builder()
                .imageProcessingInitialLayout("NCHW")
                .imageProcessingRequiredLayout("NHWC")
                .inputName("image")
                .dimensionsConfig("default", new Long[]{478L, 720L, 3L}) // Height, width, channels
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .steps(Arrays.asList(imageLoadingStep, pythonStepConfig))
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test
    public void testInferenceResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode().getBytes());
        requestSpecification.header("Content-Type", "multipart/form-data");

        File imageFile = new ClassPathResource("data/PytorchNDArrayTest.jpg").getFile();

        Response output = requestSpecification.when()
                .multiPart("image", imageFile)
                .expect().statusCode(200)
                .post("/raw/image")
                .andReturn();


        //TODO: Assertion for Numpy to be verified
        INDArray outputArray = Nd4j.createNpyFromByteArray(output.getBody().asByteArray());
        System.out.println("NumpyArrayOutput"+outputArray);
        INDArray expectedArr = ExpectedAssertUtil.NdArrayAssert("src/test/resources/Json/pytorch/PytorchImageTest.json","raw");
        System.out.println("ExpectedNumpyArrayOutput"+expectedArr);
        assertEquals(expectedArr, outputArray);
    }

    @Test
    public void testInferenceClassificationResult(TestContext context) throws Exception {

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode().getBytes());
        requestSpecification.header("Content-Type", "multipart/form-data");

        File imageFile = new ClassPathResource("data/PytorchNDArrayTest.jpg").getFile();

        Response output = requestSpecification.when()
                .multiPart("image", imageFile)
                .expect().statusCode(200)
                .post("/classification/image")
                .andReturn();

        //TODO: Assertion for Numpy to be verified
        INDArray outputArray = Nd4j.createNpyFromByteArray(output.getBody().asByteArray());
        System.out.println("NumpyArrayOutput"+outputArray);
        INDArray expectedArr = (INDArray) ExpectedAssertUtil.ProbabilitiesAssert("src/test/resources/Json/pytorch/PytorchImageTest.json");
        System.out.println("ExpectedNumpyArrayOutput"+expectedArr);
        assertEquals(expectedArr, outputArray);
    }

}
