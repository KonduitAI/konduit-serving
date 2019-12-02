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

import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.serde.binary.BinarySerde;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class MemMapArrayResultRangeVerticleTest extends ai.konduit.serving.verticles.nd4j.memmap.BaseMemMapTest {


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

    public void testArrayResultRange(TestContext context) {
        HttpClient httpClient = vertx.createHttpClient();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0);
        Async async = context.async();

        HttpClientRequest req = httpClient.post(port, "localhost", "/array/range/0/2/numpy")
                .handler(handler -> {
                    handler.bodyHandler(body -> {
                        byte[] npyArray = body.getBytes();
                        System.out.println("Found numpy array bytes with length " + npyArray.length);
                        System.out.println("Contents: " + new String(npyArray));
                        INDArray arrFromNumpy = Nd4j.createNpyFromByteArray(npyArray);
                        INDArray assertion = Nd4j.create(new float[]{1,2}).reshape(2);
                        context.assertEquals(assertion,arrFromNumpy);
                        System.out.println(arrFromNumpy);
                        async.complete();

                    });

                    handler.exceptionHandler(exception -> {
                        if(exception.getCause() != null)
                            context.fail(exception.getCause());
                    });

                }).putHeader("Content-Type","application/json")
                .putHeader("Content-Length",String.valueOf(0))
                .write("");

        async.await();
    }




}
