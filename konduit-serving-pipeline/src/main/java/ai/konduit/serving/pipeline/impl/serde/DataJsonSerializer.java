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
import ai.konduit.serving.pipeline.impl.data.ValueType;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.nd4j.shade.jackson.core.JsonGenerator;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.JsonSerializer;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.nd4j.shade.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class DataJsonSerializer extends JsonSerializer<Data> {
    @Override
    public void serialize(Data data, JsonGenerator jg, SerializerProvider sp) throws IOException {
        //TODO do we serialize in any particular order?

        jg.writeStartObject();

        List<String> l = data.keys();

        for(String s : l){
            ValueType vt = data.type(s);

            switch (vt){
                case NDARRAY:
                    break;
                case STRING:
                    String str = data.getString(s);
                    jg.writeFieldName(s);
                    jg.writeString(str);
                    break;
                case BYTES:
                    byte[] bytes = data.getBytes(s);
                    //For bytes: write as follows
                    /*
                    "myBytes" : {
                        "bytesFormat": "ARRAY" (or "BASE64")
                        bytes = ... ([1,2,3] or "<some base64 string>"
                    }
                     */

                    //TODO add option to do raw bytes array - [0, 1, 2, ...] style

                    jg.writeFieldName(s);
                    jg.writeStartObject();
                    jg.writeFieldName("format");
                    jg.writeString("BASE64");
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    jg.writeFieldName("bytes");
                    jg.writeString(base64);
                    jg.writeEndObject();
                    break;
                case IMAGE:
                    break;
                case DOUBLE:
                    writeDouble(jg, s, data.getDouble(s));
                    break;
                case INT64:
                    writeLong(jg, s, data.getLong(s));
                    break;
                case BOOLEAN:
                    boolean b = data.getBoolean(s);
                    jg.writeFieldName(s);
                    jg.writeBoolean(b);
                    break;
                case DATA:
                    //TODO Fix formatting: it doesn't correctly indent...
                    Data d = data.getData(s);
                    jg.writeFieldName(s);
                    ObjectMapper om = (ObjectMapper) jg.getCodec();
                    String dataStr = om.writeValueAsString(d);
                    jg.writeRawValue(dataStr);
                    break;
                case LIST:
                    jg.writeFieldName(s);
                    ValueType listVt = data.listType(s);
                    List<?> list = data.getList(s, listVt);
                    writeList(jg, list, listVt);
                    break;
            }
        }

        jg.writeEndObject();
    }

    private void writeDouble(JsonGenerator jg, String s, double d) throws IOException {
        jg.writeFieldName(s);
        jg.writeNumber(d);
    }

    private void writeLong(JsonGenerator jg, String s, long l) throws IOException {
        jg.writeFieldName(s);
        jg.writeNumber(l);
    }

    private void writeList(JsonGenerator jg, List<?> list, ValueType listType) throws IOException {
        int n = list.size();
        jg.writeStartArray(n);

        int i=0;
        switch (listType){
            case NDARRAY:
                break;
            case STRING:
                break;
            case BYTES:
                break;
            case IMAGE:
                break;
            case DOUBLE:
                List<Double> dList = (List<Double>)list;        //TODO checks for unsafe cast?
                double[] dArr = new double[dList.size()];
                for(Double d : dList){
                    dArr[i++] = d;
                }
                jg.writeArray(dArr, 0, dArr.length);
                break;
            case INT64:
                break;
            case BOOLEAN:
                break;
            case DATA:
                break;
            case LIST:
                break;
        }

        jg.writeEndArray();
    }
}
