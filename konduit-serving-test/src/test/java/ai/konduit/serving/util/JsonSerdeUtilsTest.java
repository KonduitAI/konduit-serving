/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.util;

import ai.konduit.serving.config.SchemaType;
import io.vertx.core.json.JsonObject;
import org.datavec.api.transform.metadata.BinaryMetaData;
import org.datavec.api.transform.schema.Schema;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class JsonSerdeUtilsTest {

    @Test
    public void testNdArraySerialization() {
        JsonObject schemaWithValues = new JsonObject();
        String ndArrayFieldName = "ndarray";
        JsonObject values = new JsonObject();
        INDArray array = Nd4j.scalar(1.0);
        values.put(ndArrayFieldName, Nd4j.toNpyByteArray(array));
        schemaWithValues.put("values",values);

        INDArray deserialized = JsonSerdeUtils.deSerializeBase64Numpy(schemaWithValues,ndArrayFieldName);
        assertEquals(array,deserialized);
    }


    @Test
    public void testDeSerializeSchemaValues() {
        // Setup
        final JsonObject schemaValues = new JsonObject();
        SchemaType[] values = SchemaType.values();
        Map<String,SchemaType> schemaTypeMap = new LinkedHashMap<>();
        Map<String,Object> deSerializedAssertion = new LinkedHashMap<>();
        for(SchemaType value : values) {
            schemaTypeMap.put(value.name(),value);
            switch(value) {
                case NDArray:
                    deSerializedAssertion.put(value.name(),Nd4j.scalar(1.0));
                    schemaValues.put(value.name(),Nd4j.toNpyByteArray(Nd4j.scalar(1.0)));
                    break;
                case Boolean:
                    deSerializedAssertion.put(value.name(),true);
                    schemaValues.put(value.name(),true);
                    break;
                case Float:
                    deSerializedAssertion.put(value.name(),1.0f);
                    schemaValues.put(value.name(),1.0f);
                    break;
                case Double:
                    deSerializedAssertion.put(value.name(),1.0);
                    schemaValues.put(value.name(),1.0);
                    break;
                case Image:
                    deSerializedAssertion.put(value.name(),new byte[]{0,1});
                    schemaValues.put(value.name(),new byte[]{0,1});
                    break;
                case Integer:
                    deSerializedAssertion.put(value.name(),1);
                    schemaValues.put(value.name(),1);
                    break;
                case String:
                    deSerializedAssertion.put(value.name(),"1.0");
                    schemaValues.put(value.name(),"1.0");
                    break;
                case Time:
                    Instant now =  Instant.now();
                    deSerializedAssertion.put(value.name(),now);
                    schemaValues.put(value.name(), now);
                    break;
                case Categorical:
                    deSerializedAssertion.put(value.name(),"cat");
                    schemaValues.put(value.name(),"cat");
                    break;
                case Bytes:
                    deSerializedAssertion.put(value.name(),new byte[]{1,0});
                    schemaValues.put(value.name(),new byte[]{1,0});
                    break;
                case Long:
                    deSerializedAssertion.put(value.name(),1L);
                    schemaValues.put(value.name(),1L);
                    break;

            }
        }

        Map<String, Object> result = JsonSerdeUtils.deSerializeSchemaValues(schemaValues, schemaTypeMap);
        for(Map.Entry<String,Object> entry : result.entrySet()) {
            //assertEquals fails on byte array comparison o_O
            if(entry.getValue() instanceof byte[]) {
                byte[] assertionArr = (byte[]) deSerializedAssertion.get(entry.getKey());
                byte[] resultVal = (byte[]) entry.getValue();
                assertArrayEquals(assertionArr,resultVal);
            }
            else {
                assertEquals(deSerializedAssertion.get(entry.getKey()),entry.getValue());
            }
        }
    }

    @Test
    public void testDeSerializeBase64Numpy() {
        INDArray numpyBinary = Nd4j.scalar(1.0);
        byte[] npy = Nd4j.toNpyByteArray(numpyBinary);
        JsonObject value = new JsonObject();
        value.put("npy",npy);
        JsonObject valuesWrapper = new JsonObject();
        valuesWrapper.put("values",value);
        // Run the test
        final INDArray result = JsonSerdeUtils.deSerializeBase64Numpy(valuesWrapper, "npy");
        assertEquals(numpyBinary,result);
    }
}
