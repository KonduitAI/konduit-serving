/*
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
 */

package ai.konduit.serving.verticles.nd4j.memmap;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.MemMapConfig;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.After;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;

public abstract class BaseMemMapTest extends BaseVerticleTest {

    @Override
    public Handler<HttpServerRequest> getRequest() {

        //should be json body of classification
        return req -> req.bodyHandler(body -> System.out.println("Finish body" + body))
                    .exceptionHandler(Throwable::printStackTrace);
    }

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return InferenceVerticle.class;
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Override
    public io.vertx.core.json.JsonObject getConfigObject() throws Exception {
        INDArray arr = Nd4j.linspace(1, 4, 4);
        File tmpFile = new File(temporary.getRoot(), "tmpfile.npy");
        byte[] save = Nd4j.toNpyByteArray(arr);
        org.apache.commons.io.FileUtils.writeByteArrayToFile(tmpFile, save);
        InferenceConfiguration inferenceConfiguration =
                InferenceConfiguration.builder()
                        .servingConfig(ServingConfig.builder()
                                .httpPort(port)
                                .build())
                        .memMapConfig(MemMapConfig.builder()
                                .arrayPath(tmpFile.getAbsolutePath())
                                .build())
                        .build();

        return new JsonObject(inferenceConfiguration.toJson());
    }
}
