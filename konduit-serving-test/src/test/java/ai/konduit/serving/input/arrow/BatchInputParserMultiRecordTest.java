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

package ai.konduit.serving.input.arrow;

import ai.konduit.serving.input.image.BatchInputParserVerticle;
import ai.konduit.serving.train.TrainUtils;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class BatchInputParserMultiRecordTest extends BaseVerticleTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return BatchInputArrowParserVerticle.class;
    }

    @Override
    public Handler<HttpServerRequest> getRequest() {
        return null;
    }

    @Override
    public JsonObject getConfigObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(BatchInputParserVerticle.INPUT_NAME_KEY, "input1");
        jsonObject.put(VerticleConstants.HTTP_PORT_KEY, port);
        return jsonObject;
    }

    @Test(timeout = 60000)
    public void runAdd(TestContext testContext) throws Exception {
        BatchInputArrowParserVerticle verticleRef = (BatchInputArrowParserVerticle) verticle;
        Schema irisInputSchema = TrainUtils.getIrisInputSchema();
        ArrowRecordWriter arrowRecordWriter = new ArrowRecordWriter(irisInputSchema);
        CSVRecordReader reader = new CSVRecordReader();
        reader.initialize(new FileSplit(new ClassPathResource("iris.txt").getFile()));
        List<List<Writable>> writables = reader.next(150);

        File tmpFile = new File(temporary.getRoot(), "tmp.arrow");
        FileSplit fileSplit = new FileSplit(tmpFile);
        arrowRecordWriter.initialize(fileSplit, new NumberOfRecordsPartitioner());
        arrowRecordWriter.writeBatch(writables);
        byte[] arrowBytes = FileUtils.readFileToByteArray(tmpFile);

        given().port(port)
                .multiPart("input1", tmpFile)
                .when().post("/")
                .then().statusCode(200);
        assertNotNull("Inputs were null. This means parsing failed.", verticleRef.getBatch());
        assertTrue(verticleRef.getBatch().length >= 1);
        assertNotNull(verticleRef.getBatch());
        assertEquals(150, verticleRef.getBatch().length);
    }
}