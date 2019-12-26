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

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.SchemaType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.transform.ColumnType;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.metadata.BinaryMetaData;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.data.ImageWritable;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

/**
 * Utils for a mix of data vec {@link Schema} manipulation
 * and configuration for {@link InferenceConfiguration}
 *
 * @author Adam Gibson
 */
public class SchemaTypeUtils {


    /**
     * Create a {@link Schema} from a {@link JsonObject}
     *  schema descriptor. The schema descriptor contains a json object of keys
     *  of type {@link ColumnType} values in the form of:
     *  name : {@link ColumnType} value
     *
     * There are 2 exceptions to this rule.
     * {@link ColumnType#NDArray} and {@link ColumnType#Categorical}
     * both are json objects.
     * {@link ColumnType#NDArray} has the form:
     * {name : shape: [], serialization type: "json" | "b64"}
     * {@link ColumnType#Categorical} has the form:
     * {categories: []}
     * {@link ColumnType#Time} has the form:
     * {timeZoneId: timeZoneId}
     *
     *
     * @param schemaDescriptor a {@link JsonObject} with the form
     *                         described above
     * @return the equivalent {@link Schema} derived from the given descriptor
     */
    public static Schema schemaFromDynamicSchemaDefinition(JsonObject schemaDescriptor) {
        Schema.Builder schemaBuilder = new Builder();
        for(String key : schemaDescriptor.fieldNames()) {
            JsonObject fieldInfo = schemaDescriptor.getJsonObject(key);
            JsonObject fieldInfoObject = fieldInfo.getJsonObject("fieldInfo");
            if(fieldInfoObject == null) {
                throw new IllegalArgumentException("Unable to find object fieldInfo!");
            }

            if(!fieldInfoObject.containsKey("type")) {
                throw new IllegalArgumentException("Illegal field info. Missing key type for identifying type of field");
            }
            //convert image to bytes and let user pre process accordingly
            String type = fieldInfoObject.getString("type");
            if(type.equals("Image")) {
                type = "Bytes";
            }
            switch(ColumnType.valueOf(type)) {
                case Boolean:
                    schemaBuilder.addColumnBoolean(key);
                    break;
                case Double:
                    schemaBuilder.addColumnDouble(key);
                    break;
                case Float:
                    schemaBuilder.addColumnFloat(key);
                    break;
                case Long:
                    schemaBuilder.addColumnLong(key);
                    break;
                case String:
                    schemaBuilder.addColumnString(key);
                    break;
                case Integer:
                    schemaBuilder.addColumnInteger(key);
                    break;
                case NDArray:
                    JsonArray shapeArr = fieldInfoObject.getJsonArray("shape");
                    long[] shape = new long[shapeArr.size()];
                    for(int i = 0; i < shape.length; i++) {
                        shape[i] = shapeArr.getLong(i);
                    }
                    schemaBuilder.addColumnNDArray(key,shape);
                    break;
                case Categorical:
                    JsonArray jsonArray = fieldInfoObject.getJsonArray("categories");
                    String[] categories = new String[jsonArray.size()];
                    for(int i = 0; i < categories.length; i++) {
                        categories[i] = jsonArray.getString(i);
                    }
                    schemaBuilder.addColumnCategorical(key,categories);
                    break;
                case Bytes:
                    ColumnMetaData columnMetaData = new BinaryMetaData(key);
                    schemaBuilder.addColumn(columnMetaData);
                    break;
                case Time:
                    TimeZone zoneById = TimeZone.getTimeZone(fieldInfoObject.getString("timeZoneId"));
                    schemaBuilder.addColumnTime(key,zoneById);
                    break;

            }
        }

        return schemaBuilder.build();
    }



    /**
     * Get record for all values
     *
     * @param values the record to get
     * @return the record
     */
    public static List<Writable> getRecord(double... values) {
        List<Writable> ret = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ret.add(new DoubleWritable(values[i]));
        }

        return ret;
    }

    /**
     * Get the column names for the input schema
     *
     * @param schema the schema to get the names for
     * @return list of column names
     */
    public static List<String> columnNames(Schema schema) {
        return schema.getColumnNames();
    }



    /**
     * Extract an ordered list
     * of the types in a given {@link Schema}
     *
     * @param schema the schema to get the types for
     * @return the schema types based on the ordering
     * of the columns in the schema
     */
    public static SchemaType[] typesForSchema(Schema schema) {
        SchemaType[] ret = new SchemaType[schema.numColumns()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = schemaTypeForColumnType(schema.getType(i));
        }

        return ret;
    }

    /**
     * Create a mapping of name {@link SchemaType}
     * based on the {@link Schema}
     *
     * @param schema the schema to decompose
     * @return the map of name to {@link SchemaType}
     */
    public static Map<String, SchemaType> typeMappingsForSchema(Schema schema) {
        Map<String, SchemaType> ret = new LinkedHashMap<>();
        for (int i = 0; i < schema.numColumns(); i++) {
            ret.put(schema.getName(i), schemaTypeForColumnType(schema.getType(i)));
        }


        return ret;
    }

    /**
     * Convert a {@link ColumnType}
     * to the equivalent {@link SchemaType}
     *
     * @param columnType the column type to convert
     * @return the schema type for the given column type
     */
    public static SchemaType schemaTypeForColumnType(ColumnType columnType) {
        return SchemaType.valueOf(columnType.name());
    }

    /**
     * Create a {@link Schema}
     * from the given {@link SchemaType}
     * and the names.
     * Note that exceptions are thrown
     * when the types are null, names are null,
     * or the 2 arguments are not the same length
     *
     * @param types the type
     * @param names the names of each column
     * @return the equivalent {@link Schema} given the types
     * and names
     */
    public static Schema toSchema(SchemaType[] types, List<String> names) {
        Preconditions.checkNotNull(types, "Please specify types");
        Preconditions.checkNotNull(names, "Please specify names.");
        Preconditions.checkState(types.length == names.size(), "Types and names must be the same length");
        Schema.Builder builder = new Schema.Builder();
        for (int i = 0; i < types.length; i++) {
            Preconditions.checkNotNull(types[i], "Type " + i + " was null!");
            switch (types[i]) {
                case NDArray:
                    builder.addColumnNDArray(names.get(i), new long[]{1, 1});
                    break;
                case String:
                    builder.addColumnString(names.get(i));
                    break;
                case Boolean:
                    builder.addColumnBoolean(names.get(i));
                    break;
                case Categorical:
                    builder.addColumnCategorical(names.get(i));
                    break;
                case Float:
                    builder.addColumnFloat(names.get(i));
                    break;
                case Double:
                    builder.addColumnDouble(names.get(i));
                    break;
                case Integer:
                    builder.addColumnInteger(names.get(i));
                    break;
                case Long:
                    builder.addColumnLong(names.get(i));
                    break;
                case Bytes:
                case Image:
                    BinaryMetaData binaryMetaData = new BinaryMetaData(names.get(i));
                    builder.addColumn(binaryMetaData);
                    break;
                case Time:
                    builder.addColumnTime(names.get(i),TimeZone.getDefault());
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type " + types[i]);

            }
        }

        return builder.build();
    }

    /**
     * Returns true if all records
     * and {@link Writable} are of type
     * {@link NDArrayWritable}
     * false otherwise
     *
     * @param records the input {@link Record} to test
     * @return true if all the records are of type {@link NDArrayWritable}
     * false otherwise
     */
    public static boolean recordsAllArrayType(org.datavec.api.records.Record[] records) {
        for (int i = 0; i < records.length; i++) {
            for (Writable writable : records[i].getRecord()) {
                if (!(writable instanceof NDArrayWritable || writable instanceof ImageWritable)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Convert an {@link INDArray}
     * batch to {@link Record}
     * input comprising of a single {@link NDArrayWritable}
     *
     * @param input the input
     * @return the equivalent output records
     */
    public static Record[] toRecords(INDArray[] input) {
        Record[] ret = new Record[input.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new org.datavec.api.records.impl.Record(
                    Arrays.asList(new NDArrayWritable(input[i]))
                    , null);
        }

        return ret;
    }

    /**
     * Convert a set of {@link Record}
     * to {@link INDArray}
     * this assumes that each "record" is
     * actually a size 1 {@link Writable} of type
     * {@link NDArrayWritable}
     *
     * @param records the records to convert
     * @return the extracted {@link INDArray}
     */
    public static INDArray[] toArrays(Record[] records) {
        INDArray[] ret = new INDArray[records[0].getRecord().size()];
        int initialLength = ret.length;
        //each ndarray
        for (int i = 0; i < initialLength; i++) {
            List<INDArray> accum = new ArrayList<>();
            //for each record
            for (Record record : records) {
                NDArrayWritable writable = (NDArrayWritable) record.getRecord().get(i);
                accum.add(writable.get());

            }

            ret[i] = Nd4j.concat(0, accum.toArray(new INDArray[0]));

        }

        return ret;
    }

}
