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
import ai.konduit.serving.output.types.NDArrayOutput;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.writable.BooleanWritable;
import org.datavec.api.writable.BytesWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.serde.json.JsonMappers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
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
        schemaWithValues.put("values", values);

        INDArray deserialized = JsonSerdeUtils.deSerializeBase64Numpy(schemaWithValues, ndArrayFieldName);
        assertEquals(array, deserialized);
    }


    @Test
    public void testDeSerializeSchemaValues() throws Exception {
        // Setup
        final JsonObject schemaValues = new JsonObject();
        SchemaType[] values = SchemaType.values();
        Map<String, SchemaType> schemaTypeMap = new LinkedHashMap<>();
        Map<String, Object> deSerializedAssertion = new LinkedHashMap<>();
        for (SchemaType value : values) {
            schemaTypeMap.put(value.name(), value);
            switch (value) {
                case NDArray:
                    deSerializedAssertion.put(value.name(), Nd4j.scalar(1.0f));
                    String write = JsonMappers.getMapper().writeValueAsString(NDArrayOutput.builder()
                            .ndArray(Nd4j.scalar(1.0f)).batchId("").build());
                    schemaValues.put(value.name(), new JsonObject(write));
                    break;
                case Boolean:
                    deSerializedAssertion.put(value.name(), true);
                    schemaValues.put(value.name(), true);
                    break;
                case Float:
                    deSerializedAssertion.put(value.name(), 1.0f);
                    schemaValues.put(value.name(), 1.0f);
                    break;
                case Double:
                    deSerializedAssertion.put(value.name(), 1.0);
                    schemaValues.put(value.name(), 1.0);
                    break;
                case Image:
                    deSerializedAssertion.put(value.name(), new byte[]{0, 1});
                    schemaValues.put(value.name(), new byte[]{0, 1});
                    break;
                case Integer:
                    deSerializedAssertion.put(value.name(), 1);
                    schemaValues.put(value.name(), 1);
                    break;
                case String:
                    deSerializedAssertion.put(value.name(), "1.0");
                    schemaValues.put(value.name(), "1.0");
                    break;
                case Time:
                    Instant now = Instant.now();
                    deSerializedAssertion.put(value.name(), now);
                    schemaValues.put(value.name(), now);
                    break;
                case Categorical:
                    deSerializedAssertion.put(value.name(), "cat");
                    schemaValues.put(value.name(), "cat");
                    break;
                case Bytes:
                    deSerializedAssertion.put(value.name(), new byte[]{1, 0});
                    schemaValues.put(value.name(), new byte[]{1, 0});
                    break;
                case Long:
                    deSerializedAssertion.put(value.name(), 1L);
                    schemaValues.put(value.name(), 1L);
                    break;

            }
        }

        Map<String, Object> result = JsonSerdeUtils.deSerializeSchemaValues(schemaValues, schemaTypeMap);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            //assertEquals fails on byte array comparison o_O
            if (entry.getValue() instanceof byte[]) {
                byte[] assertionArr = (byte[]) deSerializedAssertion.get(entry.getKey());
                byte[] resultVal = (byte[]) entry.getValue();
                assertArrayEquals(assertionArr, resultVal);
            } else {
                assertEquals(deSerializedAssertion.get(entry.getKey()), entry.getValue());
            }
        }
    }

    @Test
    public void testDeSerializeBase64Numpy() {
        INDArray numpyBinary = Nd4j.scalar(1.0);
        byte[] npy = Nd4j.toNpyByteArray(numpyBinary);
        JsonObject value = new JsonObject();
        value.put("npy", npy);
        JsonObject valuesWrapper = new JsonObject();
        valuesWrapper.put("values", value);
        // Run the test
        final INDArray result = JsonSerdeUtils.deSerializeBase64Numpy(valuesWrapper, "npy");
        assertEquals(numpyBinary, result);
    }

    @Test
    public void testCreateRecordFromJson() throws Exception {
        // Setup
        JsonObject schemaValues = new JsonObject();
        JsonObject jsonSchema = new JsonObject();
        SchemaType[] values = SchemaType.values();
        List<String> fieldNames = Arrays.stream(values).map(input -> input.name()).collect(Collectors.toList());
        for (SchemaType value : values) {
            JsonObject fieldInfo = new JsonObject();
            JsonObject topLevel = new JsonObject();
            fieldInfo.put("type",value.name());
            topLevel.put("fieldInfo",fieldInfo);
            jsonSchema.put(value.name(), topLevel);
            switch (value) {
                case NDArray:
                    fieldInfo.put("shape",new JsonArray().add(1).add(1));
                    String write = JsonMappers.getMapper().writeValueAsString(NDArrayOutput.builder().ndArray(Nd4j.scalar(1.0)).batchId("").build());
                    schemaValues.put(value.name(), new JsonObject(write));
                    break;
                case Boolean:
                    schemaValues.put(value.name(), true);
                    break;
                case Float:
                    schemaValues.put(value.name(), 1.0f);
                    break;
                case Double:
                    schemaValues.put(value.name(), 1.0);
                    break;
                case Image:
                    schemaValues.put(value.name(), new byte[]{0, 1});
                    break;
                case Integer:
                    schemaValues.put(value.name(), 1);
                    break;
                case String:
                    schemaValues.put(value.name(), "1.0");
                    break;
                case Time:
                    fieldInfo.put("timeZoneId", TimeZone.getDefault().getID());
                    Instant now = Instant.now();
                    schemaValues.put(value.name(), now);
                    break;
                case Categorical:
                    fieldInfo.put("categories",new JsonArray().add("cat"));
                    schemaValues.put(value.name(), "cat");
                    break;
                case Bytes:
                    schemaValues.put(value.name(), new byte[]{1, 0});
                    break;
                case Long:
                    schemaValues.put(value.name(), 1L);
                    break;

            }
        }

        // Run the test
        final Record result = JsonSerdeUtils.createRecordFromJson(schemaValues, jsonSchema);
        for(int i = 0; i < result.getRecord().size(); i++) {
            Writable writable = result.getRecord().get(i);
            SchemaType schemaType = values[i];
            switch(schemaType) {
                case NDArray:
                    NDArrayWritable ndArrayWritable = (NDArrayWritable) writable;
                    assertEquals(Nd4j.scalar(1.0),ndArrayWritable.get());
                    break;
                case Long:
                    assertEquals(1L,writable.toLong());
                    break;
                case Image:
                    break;
                case Integer:
                    assertEquals(1,writable.toInt());
                    break;
                case Float:
                    assertEquals(1.0f,writable.toFloat(),1e-3);
                    break;
                case Double:
                    assertEquals(1.0d,writable.toDouble(),1e-3);
                    break;
                case String:
                    assertEquals("1.0",writable.toString());
                    break;
                case Categorical:
                    assertEquals("cat",writable.toString());
                    break;
                case Bytes:
                    BytesWritable bytesWritable = (BytesWritable) writable;
                    byte[] content = bytesWritable.getContent();
                    assertArrayEquals(new byte[]{1, 0},content);
                    break;
                case Boolean:
                    BooleanWritable booleanWritable = (BooleanWritable) writable;
                    assertTrue(booleanWritable.get());
                    break;
            }
        }


        final List<String> names = Arrays.asList("name1","name2");
        final Record[] records = new Record[]{result,result};
        final JsonObject resultTest = JsonSerdeUtils.convertRecords(records, names);
        assertEquals(names.size(),resultTest.size());
        assertEquals(new HashSet<>(names),resultTest.fieldNames());
        for(String name : names) {
            JsonArray conversion = resultTest.getJsonArray(name);
            assertEquals(result.getRecord().size(),conversion.size());
            JsonObject mapped = JsonSerdeUtils.convertArray(conversion,fieldNames);
            assertEquals(schemaValues,mapped);

        }
    }

}