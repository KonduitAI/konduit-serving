/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.pipeline.impl.serde;

import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.JsonDeserializer;
import org.nd4j.shade.jackson.databind.JsonNode;
import org.nd4j.shade.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Custom JSON deserializer for Data instances<br>
 * See {@link DataJsonSerializer} for further details
 *
 * @author Alex Black
 */
public class DataJsonDeserializer extends JsonDeserializer<Data> {
    @Override
    public Data deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode n = jp.getCodec().readTree(jp);
        return deserialize(jp, n);
    }

    public Data deserialize(JsonParser jp, JsonNode n) {
        JData d = new JData();

        Iterator<String> names = n.fieldNames();
        while (names.hasNext()) {
            String s = names.next();
            JsonNode n2 = n.get(s);

            if (Data.RESERVED_KEY_METADATA.equalsIgnoreCase(s)) {
                Data meta = deserialize(jp, n2);
                d.setMetaData(meta);
            } else {
                if (n2.isTextual()) {                     //String
                    String str = n2.textValue();
                    d.put(s, str);
                } else if (n2.isDouble()) {               //Double
                    double dVal = n2.doubleValue();
                    d.put(s, dVal);
                } else if (n2.isInt() || n2.isLong()) {   //Long
                    long lVal = n2.longValue();
                    d.put(s, lVal);
                } else if (n2.isBoolean()) {              //Boolean
                    boolean b = n2.booleanValue();
                    d.put(s, b);
                } else if (n2.isArray()){
                    Pair<List<Object>, ValueType> p = deserializeList(jp, n2);
                    d.putList(s, p.getFirst(), p.getSecond());
                } else if (n2.isObject()) {
                    //Could be: Bytes, image, NDArray or Data
                    if (n2.has(Data.RESERVED_KEY_BYTES_BASE64) || n2.has(Data.RESERVED_KEY_BYTES_ARRAY)) {
                        //byte[] stored in base64 or byte[] as JSON array
                        byte[] bytes = deserializeBytes(n2);
                        d.put(s, bytes);
                    } else if (n2.has(Data.RESERVED_KEY_NDARRAY_TYPE)) {
                        //NDArray
                        d.put(s, deserializeNDArray(n2));
                    } else if (n2.has(Data.RESERVED_KEY_IMAGE_DATA)) {
                        //Image
                        d.put(s, deserializeImage(n2));
                    } else {
                        //Must be data
                        Data dInner = deserialize(jp, n2);
                        d.put(s, dInner);
                    }
                } else {
                    throw new UnsupportedOperationException("Type not yet implemented");
                }
            }
        }

        return d;
    }



    protected NDArray deserializeNDArray(JsonNode n){
        NDArrayType type = NDArrayType.valueOf(n.get(Data.RESERVED_KEY_NDARRAY_TYPE).textValue());
        ArrayNode shapeNode = (ArrayNode) n.get(Data.RESERVED_KEY_NDARRAY_SHAPE);
        long[] shape = new long[shapeNode.size()];
        for (int i = 0; i < shape.length; i++)
            shape[i] = shapeNode.get(i).asLong();
        String base64 = n.get(Data.RESERVED_KEY_NDARRAY_DATA_BASE64).textValue();
        byte[] bytes = Base64.getDecoder().decode(base64);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        SerializedNDArray ndArray = new SerializedNDArray(type, shape, bb);
        return NDArray.create(ndArray);
    }

    protected Image deserializeImage(JsonNode n2){
        String format = n2.get(Data.RESERVED_KEY_IMAGE_FORMAT).textValue();
        if(!"PNG".equalsIgnoreCase(format)){
            throw new UnsupportedOperationException("Deserialization of formats other than PNG not yet implemented");
        }
        String base64Data = n2.get(Data.RESERVED_KEY_IMAGE_DATA).textValue();
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        Png png = new Png(bytes);
        return Image.create(png);
    }

    protected byte[] deserializeBytes(JsonNode n2){
        if (n2.has(Data.RESERVED_KEY_BYTES_BASE64)) {
            //byte[] stored in base64
            JsonNode n3 = n2.get(Data.RESERVED_KEY_BYTES_BASE64);
            String base64Str = n3.textValue();
            byte[] bytes = Base64.getDecoder().decode(base64Str);
            return bytes;
        } else if (n2.has(Data.RESERVED_KEY_BYTES_ARRAY)) {
            //byte[] as JSON array
            ArrayNode n3 = (ArrayNode) n2.get(Data.RESERVED_KEY_BYTES_ARRAY);
            int size = n3.size();
            byte[] b = new byte[size];
            for (int i = 0; i < size; i++) {
                int bVal = n3.get(i).asInt();
                if (bVal < Byte.MIN_VALUE || bVal > Byte.MAX_VALUE) {
                    throw new IllegalStateException("Unable to deserialize Data from JSON: JSON contains byte[] with value outside" +
                            " of valid range [-128, 127] - value: " + bVal + " at index " + i);
                }
                b[i] = (byte) bVal;
            }
            return b;
        } else {
            throw new UnsupportedOperationException("JSON node is not a bytes node");
        }
    }

    protected Pair<List<Object>, ValueType> deserializeList(JsonParser jp, JsonNode n){
        ArrayNode an = (ArrayNode)n;
        int size = an.size();
        //TODO PROBLEM: empty list type is ambiguous!
        Preconditions.checkState(size > 0, "Unable to deserialize empty lists (not yet implemented)");
        JsonNode n3 = n.get(0);
        ValueType listType = nodeType(n3);
        List<Object> list = new ArrayList<>();
        switch (listType){
            case NDARRAY:
                for( int i=0; i<size; i++ ){
                    list.add(deserializeNDArray(n.get(i)));
                }
                break;
            case STRING:
                for( int i=0; i<size; i++ ){
                    list.add(n.get(i).textValue());
                }
                break;
            case BYTES:
                for( int i=0; i<size; i++ ){
                    list.add(deserializeBytes(n.get(i)));
                }
                break;
            case IMAGE:
                for( int i=0; i<size; i++ ){
                    list.add(deserializeImage(n.get(i)));
                }
                break;
            case DOUBLE:
                for( int i=0; i<size; i++ ){
                    list.add(n.get(i).doubleValue());
                }
                break;
            case INT64:
                for( int i=0; i<size; i++ ){
                    list.add(n.get(i).longValue());
                }
                break;
            case BOOLEAN:
                for( int i=0; i<size; i++ ){
                    list.add(n.get(i).booleanValue());
                }
                break;
            case DATA:
                for( int i=0; i<size; i++ ){
                    list.add(deserialize(jp, n.get(i)));
                }
                break;
            case LIST:
                for( int i=0; i<size; i++ ){
                    list.add(deserializeList(jp, n.get(i)));
                }
                break;
            default:
                throw new IllegalStateException("Unable to deserialize list with values of type: " + listType);
        }
        return new Pair<>(list, listType);
    }

    protected ValueType nodeType(JsonNode n){
        if (n.isTextual()) {                     //String
            return ValueType.STRING;
        } else if (n.isDouble()) {               //Double
            return ValueType.DOUBLE;
        } else if (n.isInt() || n.isLong()) {   //Long
            return ValueType.INT64;
        } else if (n.isBoolean()) {              //Boolean
            return ValueType.BOOLEAN;
        } else if (n.isArray()){
            return ValueType.LIST;
        } else if (n.isObject()) {
            //Could be: Bytes, image, NDArray or Data
            if (n.has(Data.RESERVED_KEY_BYTES_BASE64)) {
                return ValueType.BYTES;
            } else if (n.has(Data.RESERVED_KEY_BYTES_ARRAY)) {
                return ValueType.BYTES;
            } else if (n.has(Data.RESERVED_KEY_NDARRAY_TYPE)) {
                //NDArray
                return ValueType.NDARRAY;
            } else if (n.has(Data.RESERVED_KEY_IMAGE_DATA)) {
                //Image
                return ValueType.IMAGE;
            } else {
                //Must be data
                return ValueType.DATA;
            }
        } else {
            throw new UnsupportedOperationException("Type not yet implemented");
        }
    }
}
