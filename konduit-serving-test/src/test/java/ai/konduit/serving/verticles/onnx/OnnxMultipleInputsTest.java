/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019-2020 Konduit AI.
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

package ai.konduit.serving.verticles.onnx;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.TestUtils;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.model.OnnxStep;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.jayway.restassured.response.Response;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class OnnxMultipleInputsTest extends BaseVerticleTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {
        return null;
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        File model = new File(TestUtils.testResourcesStorageDir(), "inference/onnx/add.onnx");

        if (!model.exists()) {
            FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/onnx/onnx/master/onnx/backend/test/data/node/test_add/model.onnx"), model);
        }

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.NUMPY)
                .httpPort(port)
                .build();

        OnnxStep modelPipelineConfig = OnnxStep.builder()
                .path(model.getAbsolutePath())
                .inputNames(Arrays.asList("x", "y"))
                .outputNames(Collections.singletonList("sum"))
                .build();


        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineConfig)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test
    public void runAdd(TestContext testContext) throws Exception {

        long inputTensorSize = 3 * 4 * 5;

        FloatPointer inputTensorValues = new FloatPointer(inputTensorSize);
        FloatIndexer idx = FloatIndexer.create(inputTensorValues);
        for (long i = 0; i < inputTensorSize; i++)
            idx.put(i, (float) i / (inputTensorSize + 1));

        DataBuffer buffer = Nd4j.createBuffer(inputTensorValues, DataType.FLOAT, inputTensorSize, idx);

        INDArray contents = Nd4j.create(buffer);

        byte[] npyContents = Nd4j.toNpyByteArray(contents);

        File inputFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(inputFile, npyContents);

        Response response = given().port(port)
                .multiPart("x", inputFile)
                .multiPart("y", inputFile)
                .post("nd4j/numpy")
                .andReturn();

        assertEquals("Response failed", 200, response.getStatusCode());

        INDArray bodyResult = Nd4j.createNpyFromByteArray(response.getBody().asByteArray());

        assertEquals(0.032786883, bodyResult.getFloat(1), 1e-6);

        assertArrayEquals(new long[]{1, 60}, bodyResult.shape());
    }

    @After
    public void after(TestContext context) {
        super.after(context);
    }

}
