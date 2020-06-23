/*
 *
 *  * ******************************************************************************
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


package ai.konduit.serving.verticles.pmml;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.config.SchemaType;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.model.PmmlStep;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.deeplearning4j.datasets.base.IrisUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ai.konduit.serving.executioner.PipelineExecutioner.convertBatchOutput;
import static com.jayway.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class PmmlIrisTest extends BaseVerticleTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @Test
    public void testInferenceResult(TestContext context) throws Exception {
        List<DataSet> dataSets = IrisUtils.loadIris(0, 1);
        INDArray input = dataSets.get(0).getFeatures();
        Buffer inputBuffer = convertBatchOutput(input, Output.DataFormat.JSON);
        String extract = given().port(port)
                .contentType(com.jayway.restassured.http.ContentType.JSON)
                .body(inputBuffer.toString())
                .post("/classification/json")
                .then()
                .statusCode(200).extract().body().asString();
        System.out.println("Final response for pmml " + extract);
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {
        return req -> {
            //should be json body of classification
            req.bodyHandler(body -> {
                System.out.println(body.toJson());
                System.out.println("Finish body" + body);
            })
            .exceptionHandler(exception -> context.fail(exception));
        };
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        ServingConfig servingConfig = new ServingConfig()
                .httpPort(port)
                .outputDataFormat(ai.konduit.serving.config.Output.DataFormat.JSON);

        PmmlStep pmmlPipelineStep = PmmlStep.builder()
                .path(new ClassPathResource("/inference/iris/classification/IrisTree.xml").getFile().getAbsolutePath())
                .inputName("default")
                .inputColumnName("default", Arrays.asList(
                        "sepal_length",
                        "sepal_width",
                        "petal_length",
                        "petal_width"))
                .inputSchema("default", Arrays.asList(
                        SchemaType.Double,
                        SchemaType.Double,
                        SchemaType.Double,
                        SchemaType.Double))
                .outputName("default")
                .outputColumnName("default", Collections.singletonList("class"))
                .outputSchema("default", Collections.singletonList(SchemaType.String))
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .step(pmmlPipelineStep)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }
}
