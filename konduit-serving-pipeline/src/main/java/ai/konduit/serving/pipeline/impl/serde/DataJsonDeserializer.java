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

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.JData;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.JsonDeserializer;
import org.nd4j.shade.jackson.databind.JsonNode;
import org.nd4j.shade.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

public class DataJsonDeserializer extends JsonDeserializer<Data> {
    @Override
    public Data deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        JsonNode n = jp.getCodec().readTree(jp);
        return deserialize(jp, n);
    }

    public Data deserialize(JsonParser jp, JsonNode n){
        JData d = new JData();

        Iterator<String> names = n.fieldNames();
        while(names.hasNext()){
            String s = names.next();
            JsonNode n2 = n.get(s);

            if(Data.RESERVED_KEY_METADATA.equalsIgnoreCase(s)) {
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
                } else if (n2.isObject()) {
                    //Could be: Bytes, image, NDArray or Data
                    if (n2.has(Data.RESERVED_KEY_BYTES_BASE64)) {
                        //byte[] stored in base64
                        JsonNode n3 = n2.get(Data.RESERVED_KEY_BYTES_BASE64);
                        String base64Str = n3.textValue();
                        byte[] bytes = Base64.getDecoder().decode(base64Str);
                        d.put(s, bytes);
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
                        d.put(s, b);
                    } else if (n2.has(Data.RESERVED_KEY_IMAGE_DATA)) {
                        //Image
                        throw new UnsupportedOperationException("Image deserialization not yet implemented");
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
}
