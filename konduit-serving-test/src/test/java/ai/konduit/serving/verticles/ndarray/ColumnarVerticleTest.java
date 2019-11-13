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

package ai.konduit.serving.verticles.ndarray;


import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.concurrent.NotThreadSafe;

import static com.jayway.restassured.RestAssured.given;


@RunWith(VertxUnitRunner.class)
@NotThreadSafe
public class ColumnarVerticleTest extends BaseDl4JVerticalTest {
    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test(timeout = 60000)
    public void testInferenceResult(TestContext context) {
        JsonArray jsonArray = new JsonArray();
        double[] vals = {5.1,3.5,1.4,0.2};
        for(int i = 0; i < 4; i++)  {
            jsonArray.add(vals[i]);
        }


        JsonArray wrapper = new JsonArray();
        wrapper.add(jsonArray);
        given().content(wrapper.toString())
                .contentType(ContentType.JSON)
                .port(port)
                .when()
                .post("/classification/csv").andReturn();
    }


}