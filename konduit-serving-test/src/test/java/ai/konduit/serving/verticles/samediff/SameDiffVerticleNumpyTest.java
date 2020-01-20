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

package ai.konduit.serving.verticles.samediff;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.model.ModelConfig;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.model.SameDiffConfig;
import ai.konduit.serving.pipeline.step.ModelStep;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class SameDiffVerticleNumpyTest extends BaseVerticleTest {
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
        SameDiff sameDiff = SameDiff.create();
        SDVariable x = sameDiff.placeHolder("x", DataType.FLOAT,  2);
        SDVariable y = sameDiff.placeHolder("y", DataType.FLOAT,  2);
        SDVariable add = x.add("output", y);
        File tmpSameDiffFile = temporary.newFile();
        sameDiff.asFlatFile(tmpSameDiffFile);

        ServingConfig servingConfig = ServingConfig.builder()
                .outputDataFormat(Output.DataFormat.NUMPY)
                .httpPort(port)
                .build();

        SameDiffConfig modelConfig = SameDiffConfig.builder()
                .modelConfigType(
                        ModelConfigType.builder()
                                .modelType(ModelConfig.ModelType.SAMEDIFF)
                                .modelLoadingPath(tmpSameDiffFile.getAbsolutePath())
                                .build()
                ).build();

        ModelStep config = ModelStep.builder()
                .modelConfig(modelConfig)
                .inputNames(Arrays.asList("x", "y"))
                .outputNames(Arrays.asList("output"))
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(config)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }


    @Test
    public void runAdd(TestContext testContext) throws Exception {
        INDArray x = Nd4j.create(new float[]{1.0f, 2.0f});
        INDArray y = Nd4j.create(new float[]{2.0f, 3.0f});
        byte[] xNpy = Nd4j.toNpyByteArray(x);
        byte[] yNpy = Nd4j.toNpyByteArray(y);


        File xFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(xFile, xNpy);

        File yFile = temporary.newFile();
        FileUtils.writeByteArrayToFile(yFile, yNpy);


        Response response = given().port(port)
                .multiPart("x", xFile)
                .multiPart("y", yFile)
                .post("/numpy/numpy")
                .andReturn();

        assertEquals("Response failed", 200, response.getStatusCode());

        INDArray bodyResult = Nd4j.createNpyFromByteArray(response.getBody().asByteArray());
        assertArrayEquals(new long[]{2}, bodyResult.shape());
        assertEquals(Nd4j.create(new float[]{3.0f, 5.0f}), bodyResult);


    }


}
