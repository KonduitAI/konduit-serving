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

import java.util.List;
import java.util.Arrays;

import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.model.PmmlConfig;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.model.ModelConfigType;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.config.Input.DataType;
import ai.konduit.serving.config.SchemaType;
import org.nd4j.linalg.dataset.DataSet;
import ai.konduit.serving.config.Output.PredictionType;
import ai.konduit.serving.config.Output;
import  ai.konduit.serving.pipeline.PmmlPipelineStep;
import ai.konduit.serving.InferenceConfiguration;

import org.nd4j.linalg.api.ndarray.INDArray;

import io.vertx.core.json.JsonObject;
import org.junit.runner.RunWith;
import javax.annotation.concurrent.NotThreadSafe;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.TestContext;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import  io.vertx.core.AbstractVerticle;

import org.junit.Test;

import org.nd4j.linalg.io.ClassPathResource;

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
    public void testInferenceResult(TestContext context) throws Exception  {
        List<DataSet> dataSets = org.deeplearning4j.base.IrisUtils.loadIris(0, 1);
        INDArray input = dataSets.get(0).getFeatures();
        Buffer inputBuffer = convertBatchOutput(input, Output.DataType.JSON);
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
            });

            req.exceptionHandler(exception -> context.fail(exception));
        };
    }

    @Override
    public JsonObject getConfigObject() throws Exception {
        PmmlConfig pmmlConfig = PmmlConfig.builder()
                .modelConfigType(ModelConfigType.pmml(new ClassPathResource("/inference/iris/classification/irisTree.xml").getFile().getAbsolutePath()))
                .build();

        ServingConfig servingConfig = ServingConfig.builder()
                .httpPort(port)
                .inputDataType(DataType.JSON)
                .predictionType(PredictionType.CLASSIFICATION)
                .build();

        PmmlPipelineStep pmmlPipelineStep = PmmlPipelineStep.builder()
                .modelConfig(pmmlConfig)
                .inputColumnName("default", Arrays.asList(
                        "sepal_length",
                        "sepal_width",
                        "petal_length",
                        "petal_width"
                ))
                .inputSchema("default",new SchemaType[] {
                        SchemaType.Double,
                        SchemaType.Double,
                        SchemaType.Double,
                        SchemaType.Double,
                })
                .outputColumnName("default",Arrays.asList("class"))
                .outputSchema("default",new SchemaType[]{ai.konduit.serving.config.SchemaType.String})
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(servingConfig)
                .pipelineStep(pmmlPipelineStep)
                .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }
}
