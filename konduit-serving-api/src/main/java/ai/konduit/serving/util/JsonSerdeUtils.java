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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.datavec.api.records.Record;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.*;
import org.nd4j.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.time.Instant;
import java.util.*;

/**
 *
 * Contains utilities for dealing with {@link JsonObject}
 * and serializing/de serializing result numpy arrays.
 *
 * @author Adam Gibson
 */
public class JsonSerdeUtils {


    /**
     * De serialize a base 64 numpy array.
     * @param schemaWithValues a json object in the form of:
     *                         {"values : {"fieldName": base64 string}}
     * @param fieldName the field name of the numpy array to de serialize
     * @return the de serialized numpy array using {@link Nd4j#createNpyFromByteArray(byte[])}
     */
    public static INDArray deSerializeBase64Numpy(JsonObject schemaWithValues,String fieldName) {
        byte[] numpyValue  = schemaWithValues.getJsonObject("values").getBinary(fieldName);
        return Nd4j.createNpyFromByteArray(numpyValue);
    }


    /**
     * Converts a {@link JsonArray}
     * to a {@link JsonObject} to a named json object
     * @param jsonArray the json array to convert
     * @param namesToConvert the names of each value by index (order matters)
     * @return the converted json object
     */
    public static JsonObject convertArray(JsonArray jsonArray,List<String> namesToConvert) {
        Preconditions.checkState(jsonArray.size() == namesToConvert.size(),"The sizes of the json array and the input names for mapping should be the same.");
        JsonObject ret = new JsonObject();
        for(int i = 0; i < jsonArray.size(); i++) {
            ret.put(namesToConvert.get(i),jsonArray.getValue(i));
        }

        return ret;
    }

    /**
     * Convert a {@link Record} array to a
     * named dictionary
     * @param records the records to convert
     * @param names the list of the names of each record to convert
     * @return
     */
    public static JsonObject convertRecords(Record[] records,List<String> names) {
        JsonObject jsonObject = new JsonObject();
        for(int i = 0; i < records.length; i++) {
            JsonArray array = new JsonArray();
            for(Writable writable : records[i].getRecord()) {
                switch(writable.getType()) {
                    case Long:
                        array.add(writable.toLong());
                        break;
                    case Boolean:
                        BooleanWritable booleanWritable = (BooleanWritable) writable;
                        array.add(booleanWritable.get());
                        break;
                    case Double:
                        array.add(writable.toDouble());
                        break;
                    case NDArray:
                        NDArrayWritable ndArrayWritable = (NDArrayWritable) writable;
                        array.add(Nd4j.toNpyByteArray(ndArrayWritable.get()));
                        break;
                    case Int:
                        array.add(writable.toInt());
                        break;
                    case Text:
                        array.add(writable.toString());
                        break;
                    case Float:
                        array.add(writable.toFloat());
                        break;
                    case Arrow:
                        throw new UnsupportedOperationException("Arrow is an unsupported writable type.");
                    case Bytes:
                        BytesWritable bytesWritable = (BytesWritable) writable;
                        array.add(bytesWritable.getContent());
                        break;
                    case Image:
                        throw new UnsupportedOperationException("Image is an unsupported writable type.");

                }


            }

            jsonObject.put(names.get(i),array);

        }

        return jsonObject;
    }

    /**
     *
     * @param jsonDeSerializedValues
     * @param schemaTypes
     * @return
     */
    public static Record toRecord(Map<String,Object> jsonDeSerializedValues, Map<String,SchemaType> schemaTypes) {
        List<Writable> record = new ArrayList<>();
        for(Map.Entry<String,Object> entry : jsonDeSerializedValues.entrySet()) {
            switch(schemaTypes.get(entry.getKey())) {
                case NDArray:
                    INDArray arr = (INDArray) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new NDArrayWritable(arr));
                    break;
                case Image:
                    throw new UnsupportedOperationException("Image is an unsupported type!");
                case Time:
                    Instant instant = (Instant) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new Text(instant.toString()));
                    break;
                case Boolean:
                    Boolean bool = (Boolean) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new BooleanWritable(bool));
                    break;
                case Long:
                    Long l = (Long) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new LongWritable(l));
                    break;
                case Integer:
                    Integer integer = (Integer) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new IntWritable(integer));
                    break;
                case Bytes:
                    byte[] data = (byte[]) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new BytesWritable(data));
                    break;
                case Double:
                    Double d = (Double) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new DoubleWritable(d));
                    break;
                case Float:
                    Float f = (Float) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new FloatWritable(f));
                    break;
                case String:
                case Categorical:
                    String text = (String) jsonDeSerializedValues.get(entry.getKey());
                    record.add(new Text(text));
                    break;
            }
        }

        return new org.datavec.api.records.impl.Record(record,null);
    }

    /**
     * De serializes a schema using the
     * {@link JsonObject} methods
     * where schemaValues is just a set of field name -> json value
     * @param schemaValues the values of the schema
     * @param schemaTypes the schema types
     * @return a map of de serialized objects
     */
    public static Map<String,Object> deSerializeSchemaValues(JsonObject schemaValues, Map<String, SchemaType> schemaTypes) {
        Preconditions.checkState(schemaValues.fieldNames().equals(schemaTypes.keySet()),"Schema value key names are not the same as the schema types!");
        Map<String,Object> ret = new LinkedHashMap<>();
        for(String fieldName : schemaValues.fieldNames()) {
            SchemaType schemaType = schemaTypes.get(fieldName);
            switch (schemaType) {
                case NDArray:
                    INDArray arr = Nd4j.createNpyFromByteArray(schemaValues.getBinary(fieldName));
                    ret.put(fieldName,arr);
                    break;
                case Boolean:
                    ret.put(fieldName,schemaValues.getBoolean(fieldName));
                    break;
                case Long:
                    ret.put(fieldName,schemaValues.getLong(fieldName));
                    break;
                case Float:
                    ret.put(fieldName,schemaValues.getFloat(fieldName));
                    break;
                case Image:
                case Bytes:
                    ret.put(fieldName,schemaValues.getBinary(fieldName));
                    break;
                case Integer:
                    ret.put(fieldName,schemaValues.getInteger(fieldName));
                    break;
                case Double:
                    ret.put(fieldName,schemaValues.getDouble(fieldName));
                    break;
                case Time:
                    ret.put(fieldName,schemaValues.getInstant(fieldName));
                    break;
                case String:
                case Categorical:
                    ret.put(fieldName,schemaValues.getString(fieldName));
                    break;

            }
        }

        return ret;
    }

    /**
     * Converts an {@link JsonArray}
     * to a {@link INDArray} with a {@link org.nd4j.linalg.api.buffer.DataType#FLOAT}
     *
     * @param arr the {@link JsonArray} to convert
     * @return an equivalent {@link INDArray}
     */
    public static INDArray jsonToNDArray(JsonArray arr) {
        List<Integer> shapeList = new ArrayList<>();
        JsonArray currArr = arr;
        while(true) {
           shapeList.add(currArr.size());
           Object firstElement = currArr.getValue(0);
           if (firstElement instanceof JsonArray){
               currArr = (JsonArray)firstElement;
           }
           else {
               break;
           }
        }

        long[] shape = new long[shapeList.size()];
        for (int i = 0; i < shape.length; i++) {
            shape[i] = shapeList.get(i).longValue();
        }

        INDArray ndArray = Nd4j.zeros(shape);
        INDArray flatNdArray = ndArray.reshape(-1);
        int idx = 0;
        Stack<JsonArray> stack = new Stack<>();
        stack.push(arr);
        while(!stack.isEmpty()) {
            JsonArray popped = stack.pop();
            Object first = popped.getValue(0);
            if (first instanceof JsonArray) {
                for (int i = popped.size()-1; i >= 0; i--) {
                    stack.push(popped.getJsonArray(i));
                }
            }
            else{
                for (int i = 0; i < popped.size(); i++) {
                    flatNdArray.putScalar(idx++, ((Number)popped.getValue(i)).doubleValue());
                }
            }
        }

        return ndArray;
    }

    /**
     * Convert a json object to a {@link Record}
     * by dynamically creating the record based on the passed in
     * schema json.
     * See {@link SchemaTypeUtils#schemaFromDynamicSchemaDefinition(JsonObject)}
     * for more information about the format.
     * @param jsonRecord the input json record
     * @param schemaJson the json to convert to a {@link Schema} using
     *                   {@link SchemaTypeUtils#schemaFromDynamicSchemaDefinition(JsonObject)}
     *                   and {@link SchemaTypeUtils#typeMappingsForSchema(Schema)}
     * @return
     */
    public static Record createRecordFromJson(JsonObject jsonRecord, JsonObject schemaJson) {
        Schema schema1 = SchemaTypeUtils.schemaFromDynamicSchemaDefinition(schemaJson);
        Map<String, SchemaType> schemaTypeMap = SchemaTypeUtils.typeMappingsForSchema(schema1);
        Map<String,Object> deSerializedValues = deSerializeSchemaValues(jsonRecord,schemaTypeMap);
        return toRecord(deSerializedValues,schemaTypeMap);
    }
}
