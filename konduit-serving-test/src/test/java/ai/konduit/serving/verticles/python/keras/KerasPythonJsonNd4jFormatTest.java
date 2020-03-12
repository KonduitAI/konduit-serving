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
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.serde.binary.BinarySerde;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class KerasPythonJsonNd4jFormatTest extends BaseMultiNumpyVerticalTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
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
        String pythonPath = Arrays.stream(cachePackages())
                .filter(Objects::nonNull)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));

        String pythonCodePath = new ClassPathResource("scripts/keras/KerasJsonTest.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCodePath(pythonCodePath)
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonInput("JsonInput", PythonType.TypeName.STR.name())
                //.pythonOutput("score", PythonType.TypeName.LIST.name())
                .pythonOutput("output_array", PythonType.TypeName.NDARRAY.name())
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

        File json = new ClassPathResource("Json/IrisY.json").getFile();
        jsonObject.put("JsonInput", json.getAbsolutePath());
        requestSpecification.body(jsonObject.encode());

        requestSpecification.header("Content-Type", "application/json");
        Response output = requestSpecification.when()
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/raw/json")
                .andReturn();

        System.out.println(output);

       // INDArray arr=output.asByteArray();
        File outputImagePath = new File(
                "src/main/resources/data/test-nd4j-output.zip");
      //INDArray outputArray = Nd4j.createN((output.getBody().asByteArray()));
        FileUtils.writeStringToFile(outputImagePath, output.toString(), Charset.defaultCharset());
        System.out.println(BinarySerde.readFromDisk(outputImagePath));
        INDArray outputArray = BinarySerde.readFromDisk(outputImagePath);
        InputStream expectedIS = new FileInputStream("src/test/resources/Json/keras/KerasJsonTest.json");
        String encodedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);
        List<Float> expectedObj = new JsonObject(encodedText).getJsonObject("raw").getJsonArray("score").getList();
        INDArray expectedArr = Nd4j.create(expectedObj);
        assertEquals(expectedArr,outputArray);
    }

    @Test(timeout = 60000)
    @Ignore
    public void testInferenceClassificationResult(TestContext context) throws Exception {

        this.context = context;

        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();

        File json = new ClassPathResource("Json/IrisY.json").getFile();
        jsonObject.put("JsonInput", json.getAbsolutePath());
        requestSpecification.body(jsonObject.encode());

        requestSpecification.header("Content-Type", "application/json");
        String output = requestSpecification.when()
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("/classification/json").then()
                .extract()
                .body().asString();

        File outputImagePath = new File(
                "src/main/resources/data/test-nd4j-output.zip");
        FileUtils.writeStringToFile(outputImagePath, output, Charset.defaultCharset());
        System.out.println(BinarySerde.readFromDisk(outputImagePath));
        INDArray outputArray = BinarySerde.readFromDisk(outputImagePath);
        InputStream expectedIS = new FileInputStream("src/test/resources/Json/keras/KerasJsonTest.json");
        String encodedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);
        List<Float> expectedObj = new JsonObject(encodedText).getJsonObject("classification").getJsonArray("output_array").getList();
        INDArray expectedArr = Nd4j.create(expectedObj);
        assertEquals(expectedArr, outputArray);
    }
}
