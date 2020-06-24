/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
import ai.konduit.serving.output.types.NDArrayOutput;
import ai.konduit.serving.pipeline.step.model.OnnxStep;
import ai.konduit.serving.util.ObjectMappers;
import ai.konduit.serving.util.image.NativeImageLoader;
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
import org.datavec.image.data.Image;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class OnnxMultipleOutputsTest extends BaseVerticleTest {

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
        File model = new File(TestUtils.testResourcesStorageDir(), "inference/onnx/facedetector.onnx");

        if (!model.exists()) {
            FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/Linzaer/Ultra-Light-Fast-Generic-Face-Detector-1MB/master/models/onnx/version-RFB-320.onnx"), model);
        }

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.JSON)
                .httpPort(port)
                .build();

        OnnxStep modelPipelineConfig = OnnxStep.builder()
                .path(model.getAbsolutePath())
                .inputNames(Collections.singletonList("input"))
                .outputNames(Arrays.asList("scores", "boxes"))
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(modelPipelineConfig)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }

    @Test
    public void runFaceDetector(TestContext testContext) throws Exception {
        File imageFile = Paths.get(new ClassPathResource(".").getFile().getAbsolutePath(), "inference/onnx/data/1.jpg").toFile();

        if (!imageFile.exists()) {
            FileUtils.copyURLToFile(new URL("https://github.com/KonduitAI/konduit-serving-examples/raw/master/data/facedetector/1.jpg"), imageFile);
        }
        NativeImageLoader nativeImageLoader = new NativeImageLoader(240, 320);
        Image image = nativeImageLoader.asImageMatrix(imageFile);

        INDArray contents = image.getImage();

        byte[] npyContents = Nd4j.toNpyByteArray(contents);

        File inputFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(inputFile, npyContents);

        Response response = given().port(port)
                .multiPart("input", inputFile)
                .body(npyContents)
                .post("nd4j/numpy")
                .andReturn();

        assertEquals("Response failed", 200, response.getStatusCode());

        JsonObject output = new JsonObject(response.asString());

        assertTrue(output.containsKey("scores"));
        assertTrue(output.containsKey("boxes"));

        INDArray scores = ObjectMappers.fromJson(output.getJsonObject("scores").encode(), NDArrayOutput.class).getNdArray();
        assertEquals(0.9539676, scores.getFloat(0), 1e-6);
        assertArrayEquals(new long[]{1, 8840}, scores.shape());

        INDArray boxes = ObjectMappers.fromJson(output.getJsonObject("boxes").encode(), NDArrayOutput.class).getNdArray();
        assertEquals(0.002913665, boxes.getFloat(0), 1e-6);
        assertArrayEquals(new long[]{1, 17680}, boxes.shape());
    }
}
