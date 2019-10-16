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

package ai.konduit.serving.input.image;

import ai.konduit.serving.verticles.BaseVerticleTest;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nd4j.linalg.io.ClassPathResource;

import javax.annotation.concurrent.NotThreadSafe;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class BatchInputParserTest extends BaseVerticleTest {

    @Override
    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return BatchInputParserVerticle.class;
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
        BatchInputParserVerticle verticleRef = (BatchInputParserVerticle) verticle;
        given().port(port)
                .multiPart("input1", new ClassPathResource("data/5.png").getFile())
                .when().post("/")
                .then().statusCode(200);
        assertNotNull("Inputs were null. This means parsing failed.", verticleRef.getBatch());
        assertTrue(verticleRef.getBatch().length >= 1);
        assertNotNull(verticleRef.getBatch());
    }
}