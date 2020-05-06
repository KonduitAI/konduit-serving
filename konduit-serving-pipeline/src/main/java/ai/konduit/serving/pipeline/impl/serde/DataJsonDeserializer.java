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
            if(n2.isTextual()){                     //String
                String str = n2.textValue();
                d.put(s, str);
            } else if(n2.isDouble()){               //Double
                double dVal = n2.doubleValue();
                d.put(s, dVal);
            } else if(n2.isInt() || n2.isLong()){   //Long
                long lVal = n2.longValue();
                d.put(s, lVal);
            } else if(n2.isBoolean()) {              //Boolean
                boolean b = n2.booleanValue();
                d.put(s, b);
            } else if(n2.isObject() && n2.has("format")) {
                String format = n2.get("format").textValue();
                if ("BASE64".equalsIgnoreCase(format)) {
                    String str = n2.get("bytes").textValue();
                    byte[] bytes = Base64.getDecoder().decode(str);
                    d.put(s, bytes);
                } else {
                    ArrayNode n3 = (ArrayNode) n2.get("bytes");
                    int size = n3.size();
                    byte[] b = new byte[size];
                    for (int i = 0; i < size; i++) {
                        b[i] = (byte) n3.get(i).asInt();        //TODO range checks?
                    }
                    d.put(s, b);
                }
            } else if(n2.isObject()){
                ///TODO - potential bug - nested Data instance when Data has key "format" - ambiguous with byte[] encoding
                Data dInner = deserialize(jp, n2);
                d.put(s, dInner);
            } else {
                throw new UnsupportedOperationException("Type not yet implemented");
            }
        }

        return d;
    }
}
