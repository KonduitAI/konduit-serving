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
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.PythonStep;
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
import org.datavec.python.PythonType;
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
import static junit.framework.TestCase.assertEquals;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class ScikitLearnPythonNdArrayRegressionTest extends BaseMultiNumpyVerticalTest {

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

        String pythonCodePath = new ClassPathResource("scripts/scikitlearn/ScikitLearnRegression.py").getFile().getAbsolutePath();

        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonCodePath(pythonCodePath)
                .pythonInput("inputData", PythonType.TypeName.NDARRAY.name())
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

    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) throws Exception {
        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);
        JsonObject jsonObject = new JsonObject();

        //Preparing input NDArray
        INDArray arr = Nd4j.create(new double[]{1462.00000,20.0000000,81.0000000,14267.0000,6.00000000,6.00000000,1958.00000,1958.00000,108.000000,923.000000,0.00000000,406.000000,1329.00000,1329.00000,0.00000000,0.00000000 ,1329.00000,0.00000000,0.00000000,1.00000000, 1.00000000,3.00000000,1.00000000,6.00000000, 0.00000000,1958.00000,1.00000000,312.000000, 393.000000,36.0000000,0.00000000,0.00000000, 0.00000000,0.00000000,12500.0000,6.00000000, 2010.00000,0.00000000,0.00000000,0.00000000, 1.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,1.00000000,1.00000000,68493.1507, 1.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,1.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 1.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 1.00000000,0.00000000,0.00000000,68493.1507, 68493.1507,1369.86301,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,1.00000000,5479.45205,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,68493.1507,1.00000000,68493.1507, 68493.1507,68493.1507,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 68493.1507,0.00000000,0.00000000,1369.86301, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,68493.1507,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,1.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 1.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,68493.1507, 1.00000000,0.00000000,0.00000000,1369.86301, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,1.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,68493.1507, 1.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,1.00000000,2054.79452,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,1.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 1369.86301,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,68493.1507,0.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,0.00000000, 0.00000000,0.00000000,1.00000000,0.00000000, 0.00000000,0.00000000,0.00000000,1.00000000, 0.00000000
        }, 1, 289);

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
                .post("/regression/nd4j").then()
                .extract()
                .body().asString();

        JsonObject jsonObject1 = new JsonObject(response);
        JsonObject ndarraySerde = jsonObject1.getJsonObject("default");
        JsonArray values = ndarraySerde.getJsonArray("values");
        double[][] nd = ObjectMapperHolder.getJsonMapper().readValue(values.toString(), double[][].class);
        INDArray outputArray = Nd4j.create(nd);
        double outpuValue = values.getJsonArray(0).getDouble(0);
        assertEquals(outputArray.getDouble(0), outpuValue);
    }

}
