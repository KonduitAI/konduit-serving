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

package ai.konduit.serving.util;

import ai.konduit.serving.config.SchemaType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.metadata.BinaryMetaData;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.writable.NDArrayWritable;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class SchemaTypeUtilsTest {

    @Test
    public void testSchemaFromDynamicSchemaDefinition() {
        JsonObject schema = new JsonObject();
        Schema.Builder schemaBuilder = new Builder();
        for(int i = 0; i < SchemaType.values().length; i++) {
            String columnName = SchemaType.values()[i].name().toLowerCase();
            switch(SchemaType.values()[i]) {
                case NDArray:
                    JsonObject fieldInfoNdArray = new JsonObject();
                    fieldInfoNdArray.put("shape",new JsonArray().add(1).add(1));
                    fieldInfoNdArray.put("type",SchemaType.values()[i].name());
                    JsonObject wrapperFieldInfo = new JsonObject();
                    wrapperFieldInfo.put("fieldInfo",fieldInfoNdArray);
                    schema.put(columnName,wrapperFieldInfo);
                    schemaBuilder.addColumnNDArray("ndarray",new long[]{1,1});
                    break;
                case Categorical:
                    JsonObject categoryDescriptor = new JsonObject();
                    JsonObject fieldInfo = new JsonObject();
                    fieldInfo.put("categories",new JsonArray().add("cat1").add("cat2"));
                    fieldInfo.put("type",SchemaType.values()[i].name());
                    categoryDescriptor.put("fieldInfo",fieldInfo);
                    schema.put(columnName,categoryDescriptor);
                    schemaBuilder.addColumnCategorical("categorical","cat1","cat2");
                    break;
                case Image:
                case Bytes:
                    BinaryMetaData binaryMetaData = new BinaryMetaData(SchemaType.values()[i].name().toLowerCase());
                    schemaBuilder.addColumn(binaryMetaData);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;

                case Boolean:
                    schemaBuilder.addColumnBoolean(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;
                case Double:
                    schemaBuilder.addColumnDouble(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;
                case Float:
                    schemaBuilder.addColumnFloat(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;
                case Long:
                    schemaBuilder.addColumnLong(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;

                case Integer:
                    schemaBuilder.addColumnInteger(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;
                case Time:
                    schemaBuilder.addColumnTime(columnName, TimeZone.getDefault());
                    JsonObject fieldInfoTime = new JsonObject();
                    fieldInfoTime.put("type","Time");
                    fieldInfoTime.put("timeZoneId",TimeZone.getDefault().getID());
                    JsonObject fieldInfoWrapper = new JsonObject();
                    fieldInfoWrapper.put("fieldInfo",fieldInfoTime);
                    schema.put("time",fieldInfoWrapper);
                    break;
                case String:
                    schemaBuilder.addColumnString(columnName);
                    schema.put(SchemaType.values()[i].name().toLowerCase(),
                            new JsonObject().put("fieldInfo",
                                    new JsonObject().put("type",SchemaType.values()[i].name())));
                    break;

                default:
                    throw new UnsupportedOperationException("Type " + SchemaType.values()[i].name() + " is unsupported!");

            }

        }

        Schema assertionSchema = schemaBuilder.build();
        Schema schema1 = SchemaTypeUtils.schemaFromDynamicSchemaDefinition(schema);
        assertEquals(assertionSchema,schema1);

    }

    @Test
    public void testTypeMappingsForSchema() {
        Schema.Builder schemaBuilder = new Schema.Builder();
        schemaBuilder.addColumnInteger("int");
        schemaBuilder.addColumnLong("long");
        schemaBuilder.addColumnNDArray("ndarray",new long[]{1,1});
        schemaBuilder.addColumnString("string");
        schemaBuilder.addColumnCategorical("categorical","cat1","cat2");
        schemaBuilder.addColumnFloat("float");
        schemaBuilder.addColumnDouble("double");
        schemaBuilder.addColumnBoolean("boolean");

        Schema convert = schemaBuilder.build();
        final Map<String, SchemaType> result = SchemaTypeUtils.typeMappingsForSchema(convert);

    }

    @Test
    public void testToSchema() {
        SchemaType[] values = SchemaType.values();
        Schema.Builder schemaBuilder = new Schema.Builder();

        final List<String> names = new ArrayList<>();
        for(SchemaType value : values) {
            names.add(value.name());
            switch(value) {
                case NDArray:
                    schemaBuilder.addColumnNDArray(value.name(),new long[]{1,1});
                    break;
                case Boolean:
                    schemaBuilder.addColumnBoolean(value.name());
                     break;
                case Float:
                    schemaBuilder.addColumnFloat(value.name());
                    break;
                case Double:
                    schemaBuilder.addColumnDouble(value.name());
                    break;
                case Image:
                    BinaryMetaData binaryMetaDataImage = new BinaryMetaData(value.name());
                    schemaBuilder.addColumn(binaryMetaDataImage);
                    break;
                case Integer:
                    schemaBuilder.addColumnInteger(value.name());
                    break;
                case String:
                    schemaBuilder.addColumnString(value.name());
                    break;
                case Time:
                    schemaBuilder.addColumnTime(value.name(),TimeZone.getDefault());
                    break;
                case Categorical:
                    schemaBuilder.addColumnCategorical(value.name());
                    break;
                case Bytes:
                    BinaryMetaData binaryMetaData = new BinaryMetaData(value.name());
                    schemaBuilder.addColumn(binaryMetaData);
                    break;
                case Long:
                    schemaBuilder.addColumnLong(value.name());
                    break;

            }
        }


        Schema expected = schemaBuilder.build();
        // Run the test
        final Schema result = SchemaTypeUtils.toSchema(values, names);

        // Verify the results
        assertEquals(expected, result);
    }

    @Test
    public void testSchemaTypeForColumnType() {
        for(SchemaType schemaType : SchemaType.values())
            if(schemaType != SchemaType.Image)
                assertEquals(schemaType, SchemaTypeUtils.schemaTypeForColumnType(ColumnType.valueOf(schemaType.name())));
    }

    @Test
    public void testToArrays() {
        Record[] inputs = new Record[2];
        for (int i = 0; i < inputs.length; i++)
            inputs[i] = new org.datavec.api.records.impl.Record(Arrays.asList(
                    new NDArrayWritable(Nd4j.scalar(1.0)),
                    new NDArrayWritable(Nd4j.scalar(1.0))
            ), null);

        INDArray[] indArrays = SchemaTypeUtils.toArrays(inputs);
    }

}
