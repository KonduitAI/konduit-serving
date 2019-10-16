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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.JsonExpanderTransform;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.writable.Text;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JsonExpanderTransformStepRunnerTest {

    @Test
    public void testJsonExpansionObject() {
        JsonExpanderTransformStepRunner runner = new JsonExpanderTransformStepRunner(new JsonExpanderTransform());
        Record[] input = new Record[1];
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("first",1.0);
        jsonObject.put("second","hello world");
        input[0] = new org.datavec.api.records.impl.Record(
                Arrays.asList(new Text(jsonObject.encodePrettily()))
        ,null);

        Record[] transform = runner.transform(input);
        assertEquals(1,transform.length);
        assertEquals(2,transform[0].getRecord().size());
        assertEquals(1.0,transform[0].getRecord().get(0).toDouble(),1e-1);
        assertEquals("hello world",transform[0].getRecord().get(1).toString());
    }

    @Test
    public void testJsonExpansionObjectArray() {
        JsonExpanderTransformStepRunner runner = new JsonExpanderTransformStepRunner(new JsonExpanderTransform());
        Record[] input = new Record[1];
        JsonArray inputArraysJson = new JsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("first",1.0);
        jsonObject.put("second","hello world");
        inputArraysJson.add(jsonObject);

        input[0] = new org.datavec.api.records.impl.Record(
                Arrays.asList(new Text(inputArraysJson.encodePrettily()))
                ,null);

        Record[] transform = runner.transform(input);
        assertEquals(1,transform.length);
        assertEquals(2,transform[0].getRecord().size());
        assertEquals(1.0,transform[0].getRecord().get(0).toDouble(),1e-1);
        assertEquals("hello world",transform[0].getRecord().get(1).toString());
    }

}
