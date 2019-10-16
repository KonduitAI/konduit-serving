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

package ai.konduit.serving.verticles.nd4j.memmap;

import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.BaseVerticleTest;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class MemMapSpecificVerticleTest extends BaseVerticleTest {
    private INDArray unkVector = Nd4j.linspace(1,4,4).addi(2);

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return MemMapVerticle.class;
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }



    @Override
    public Handler<HttpServerRequest> getRequest() {
        Handler<HttpServerRequest> ret = new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                //should be json body of classification
                req.bodyHandler(body -> {
                    System.out.println("Finish body" + body);
                });

                req.exceptionHandler(exception -> {
                    exception.printStackTrace();
                });




            }
        };

        return ret;
    }


    @Test(timeout = 60000)

    public void testArrayResultRangeJson(TestContext context) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(-1);
        jsonArray.add(1);
        Response response = given().contentType(ContentType.JSON)
                .content(jsonArray.toString())
                .port(port)
                .post("/array/indices/numpy")
                .andReturn();
        byte[] content = response.getBody().asByteArray();
        INDArray numpyTest = Nd4j.createNpyFromByteArray(content).reshape(2,4);
        INDArray first = Nd4j.create(new float[]{3,4,5,6,5,6,7,8}).reshape(2,4);
        assertEquals(2,numpyTest.rows());
        assertEquals(unkVector,numpyTest.slice(0));
        assertEquals(first,numpyTest);
    }


    @Override
    public JsonObject getConfigObject() throws Exception {
        JsonObject config = new JsonObject();
        File unkVectorPath = temporary.newFile();
        Nd4j.writeAsNumpy(unkVector,unkVectorPath);
        config.put(VerticleConstants.MEM_MAP_VECTOR_PATH,unkVectorPath.getAbsolutePath());
        config.put("httpPort",String.valueOf(port));
        INDArray arr = Nd4j.linspace(1,8,8).reshape(2,4);
        File tmpFile = new File(temporary.getRoot(),"tmpfile.npy");
        byte[] save = Nd4j.toNpyByteArray(arr);
        FileUtils.writeByteArrayToFile(tmpFile,save);
        config.put(MemMapVerticle.ARRAY_URL,tmpFile.getAbsolutePath());
        return config;
    }
}
