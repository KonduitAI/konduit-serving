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
import ai.konduit.serving.input.conversion.ConverterArgs;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.handlers.converter.multi.converter.impl.arrow.ArrowBinaryInputAdapter;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import ai.konduit.serving.verticles.numpy.tensorflow.BaseMultiNumpyVerticalTest;
import com.jayway.restassured.specification.RequestSpecification;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.partition.NumberOfRecordsPartitioner;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.datavec.arrow.recordreader.ArrowRecordWriter;
import org.datavec.arrow.recordreader.ArrowWritableRecordBatch;
import org.datavec.python.PythonVariables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class PythonArrowInputTest extends BaseMultiNumpyVerticalTest {

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

        String pythonCodePath = new ClassPathResource("scripts/custom/InputOutputPythonScripts.py").getFile().getAbsolutePath();
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCodePath(pythonCodePath)
                .pythonPath(pythonPath)
                .pythonInput("inputVar", PythonVariables.Type.NDARRAY.name())
                .pythonOutput("output", PythonVariables.Type.NDARRAY.name())
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

        this.context = context;
        RequestSpecification requestSpecification = given();
        requestSpecification.port(port);

        Schema irisInputSchema = TrainUtils.getIrisInputSchema();
        ArrowRecordWriter arrowRecordWriter = new ArrowRecordWriter(irisInputSchema);
        CSVRecordReader reader = new CSVRecordReader();
        reader.initialize(new FileSplit(new ClassPathResource("iris.txt").getFile()));
        List<List<Writable>> writables = reader.next(150);
        System.out.println("writables---" + writables);

        File tmpFile = new File(temporary.getRoot(), "tmp.arrow");
        System.out.println("tmpFile" + tmpFile);
        FileSplit fileSplit = new FileSplit(tmpFile);
        arrowRecordWriter.initialize(fileSplit, new NumberOfRecordsPartitioner());
        arrowRecordWriter.writeBatch(writables);

        byte[] arrowBytes = FileUtils.readFileToByteArray(tmpFile);
        Buffer buffer = Buffer.buffer(arrowBytes);

        ArrowBinaryInputAdapter arrowBinaryInputAdapter = new ArrowBinaryInputAdapter();
        ArrowWritableRecordBatch convert = arrowBinaryInputAdapter.convert(buffer, ConverterArgs.builder().schema(irisInputSchema).build(), null);

        //  assertEquals(writables.size(), convert.size());

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