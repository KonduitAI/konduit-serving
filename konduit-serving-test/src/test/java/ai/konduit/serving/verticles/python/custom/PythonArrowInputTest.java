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

package ai.konduit.serving.verticles.python.custom;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.miscutils.PythonPathInfo;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.verticles.numpy.tensorflow.BaseMultiNumpyVerticalTest;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.partition.NumberOfRecordsPartitioner;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.arrow.ArrowConverter;
import org.datavec.arrow.recordreader.ArrowRecordWriter;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.datavec.python.PythonType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.primitives.Pair;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
@Ignore
public class PythonArrowInputTest extends BaseMultiNumpyVerticalTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {

        return req ->
                //should be json body of classification
                req.bodyHandler(body -> {
                    System.out.println(body.toJson());
                    System.out.println("Finish body" + body);
                }).exceptionHandler(exception -> context.fail(exception));
    }

    @Override
    public JsonObject getConfigObject() throws Exception {

        String pythonCodePath = new ClassPathResource("scripts/custom/InputOutputPythonScripts.py").getFile().getAbsolutePath();
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCodePath(pythonCodePath)
                .pythonPath(PythonPathInfo.getPythonPath())
                .pythonInput("inputVar", PythonType.TypeName.NDARRAY.name())
                .pythonOutput("output", PythonType.TypeName.NDARRAY.name())
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
    public void testInferenceResult(TestContext testContext) throws Exception {

        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        Schema customSchema = new Schema.Builder()
                .addColumnNDArray("inputVar", new long[] {10, 10, 10})
                .build();
        ArrowRecordWriter arrowRecordWriter = new ArrowRecordWriter(customSchema);

        File tmpFile = new File(temporary.getRoot(), "tmp.arrow");
        System.out.println("tmpFile" + tmpFile);
        FileSplit fileSplit = new FileSplit(tmpFile);
        arrowRecordWriter.initialize(fileSplit, new NumberOfRecordsPartitioner());
        arrowRecordWriter.writeBatch(
                Collections.singletonList(
                        Collections.singletonList(
                                new NDArrayWritable(Nd4j.ones(10, 10, 10))
                        )
                ));

        Pair<Schema, ArrowWritableRecordBatch> output1 = ArrowConverter.readFromFile(tmpFile);
        System.out.println(output1.getValue().get(0));

        JsonObject jsonObject = new JsonObject();
        requestSpecification.body(jsonObject.encode().getBytes());
        requestSpecification.header("Content-Type", "multipart/form-data");
        // TODO: Need to check the output format
        String output = requestSpecification.when()
                .multiPart("default", tmpFile)
                .expect().statusCode(200)
                .body(not(isEmptyOrNullString()))
                .post("raw/arrow").then()
                .extract()
                .body().asString();

        System.out.println("output-----------" + output);
    }
}