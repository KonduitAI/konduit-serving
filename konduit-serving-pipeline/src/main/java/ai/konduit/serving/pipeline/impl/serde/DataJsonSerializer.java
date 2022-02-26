/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
import ai.konduit.serving.pipeline.impl.data.box.BBoxCHW;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.impl.format.JavaNDArrayFormats;
import org.nd4j.shade.jackson.core.JsonGenerator;
import org.nd4j.shade.jackson.databind.JsonSerializer;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.nd4j.shade.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;

/**
 * Custom JSON serialization for Data instances.
 * <p>
 * This is used for ALL Data implementations - and encodes the canonical JSON format that Konduit Serving will use everywhere
 * <p>
 * Note that the JSON (and YAML) format is considered part of the public API, hence for optimal usability we are using
 * a custom JSON serializer to precisely control the format.<br>
 * Other JSON options exist (using standard Jackson serializers/deserializers, or Protobuf's JSON format) but a manual approach
 * provides full control over the exact format
 *
 * @author Alex Black
 */
public class DataJsonSerializer extends JsonSerializer<Data> {

    @Override
    public void serialize(Data data, JsonGenerator jg, SerializerProvider sp) throws IOException {
        //TODO do we serialize in any particular order?

        jg.writeStartObject();

        List<String> l = data.keys();

        for (String s : l) {
            ValueType vt = data.type(s);
            jg.writeFieldName(s);

            switch (vt) {
                case NDARRAY:
                    NDArray n = data.getNDArray(s);
                    writeNDArray(jg, n);
                    break;
                case STRING:
                    String str = data.getString(s);
                    jg.writeString(str);
                    break;
                case BYTES:
                    writeBytes(jg, data.getBytes(s));
                    break;
                case IMAGE:
                    writeImage(jg, data.getImage(s));
                    break;
                case DOUBLE:
                    writeDouble(jg, data.getDouble(s));
                    break;
                case INT64:
                    writeLong(jg, data.getLong(s));
                    break;
                case BOOLEAN:
                    boolean b = data.getBoolean(s);
                    jg.writeBoolean(b);
                    break;
                case DATA:
                    Data d = data.getData(s);
                    writeNestedData(jg, d);
                    break;
                case LIST:
                    /*
                    Format:
                    "myList" : ["x", "y", "z"]
                     */
                    ValueType listVt = data.listType(s);
                    List<?> list = data.getList(s, listVt);
                    writeList(jg, list, listVt);
                    break;
                case BOUNDING_BOX:
                    BoundingBox bb = data.getBoundingBox(s);
                    writeBB(jg, bb);
                    break;
                case POINT:
                    Point p = data.getPoint(s);
                    writePoint(jg, p);
                    break;
                case BYTEBUFFER:
                    ByteBuffer byteBuffer = data.getByteBuffer(s);
                    writeBytes(jg,byteBuffer);
                    break;
                default:
                    throw new IllegalStateException("Value type not yet supported/implemented: " + vt);
            }
        }

        if (data.getMetaData() != null) {
            Data md = data.getMetaData();
            jg.writeFieldName(Data.RESERVED_KEY_METADATA);
            writeNestedData(jg, md);
        }

        jg.writeEndObject();
    }

    private void writeNestedData(JsonGenerator jg, Data data) throws IOException {
        ObjectMapper om = (ObjectMapper) jg.getCodec();
        String dataStr = om.writeValueAsString(data);
        jg.writeRawValue(dataStr);
    }

    private void writeBytes(JsonGenerator jg, ByteBuffer bytes) throws IOException {
        //TODO add option to do raw bytes array - [0, 1, 2, ...] style
        jg.writeStartObject();
        jg.writeFieldName(Data.RESERVED_KEY_BYTEBUFFER_BASE64);
        if(bytes.hasArray()) {
            String base64 = Base64.getEncoder().encodeToString(bytes.array());
            jg.writeString(base64);
            jg.writeEndObject();
        }
        else {
            byte[] bytesArr = new byte[bytes.capacity()];
            bytes.get(bytesArr.length);
            String base64 = Base64.getEncoder().encodeToString(bytesArr);
            jg.writeString(base64);
            jg.writeEndObject();
        }

    }

    private void writeBytes(JsonGenerator jg, byte[] bytes) throws IOException {
        //TODO add option to do raw bytes array - [0, 1, 2, ...] style
        jg.writeStartObject();
        jg.writeFieldName(Data.RESERVED_KEY_BYTES_BASE64);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        jg.writeString(base64);
        jg.writeEndObject();
    }

    private void writeDouble(JsonGenerator jg, double d) throws IOException {
        jg.writeNumber(d);
    }

    private void writeLong(JsonGenerator jg, long l) throws IOException {
        jg.writeNumber(l);
    }

    private void writeImage(JsonGenerator jg, Image i) throws IOException {
        Png png = i.getAs(Png.class);
        byte[] imgData = png.getBytes();
        jg.writeStartObject();
        jg.writeFieldName(Data.RESERVED_KEY_IMAGE_FORMAT);
        jg.writeString("PNG");      //TODO No magic constant
        jg.writeFieldName(Data.RESERVED_KEY_IMAGE_DATA);
        String base64 = Base64.getEncoder().encodeToString(imgData);
        jg.writeString(base64);
        jg.writeEndObject();
    }

    private void writeNDArray(JsonGenerator jg, NDArray n) throws IOException {
            jg.writeStartObject();

            SerializedNDArray sn = n.getAs(SerializedNDArray.class);
            NDArrayType type = sn.getType();
            long[] shape = sn.getShape();
            jg.writeFieldName(Data.RESERVED_KEY_NDARRAY_TYPE);
            jg.writeString(type.toString());
            jg.writeFieldName(Data.RESERVED_KEY_NDARRAY_SHAPE);
            jg.writeArray(shape, 0, shape.length);

            ByteBuffer bb = sn.getBuffer();
            bb.rewind();
            byte[] array;
            if (bb.hasArray()) {
                array = bb.array();
            } else {
                int size = bb.remaining();
                array = new byte[size];
                for (int i = 0; i < size; i++) {
                    array[i] = bb.get(i);
                }
            }

            String base64 = Base64.getEncoder().encodeToString(array);
            jg.writeFieldName(Data.RESERVED_KEY_NDARRAY_DATA_ARRAY);
            jg.writeString(base64);
            jg.writeEndObject();

    }

    public static void writeBB(JsonGenerator jg, BoundingBox bb) throws IOException {
        //We'll keep it in the original format, if possible - but encode it as a X/Y format otherwise
        jg.writeStartObject();
        if(bb instanceof BBoxCHW){
            BBoxCHW b = (BBoxCHW)bb;
            jg.writeFieldName(Data.RESERVED_KEY_BB_CX);
            jg.writeNumber(b.cx());
            jg.writeFieldName(Data.RESERVED_KEY_BB_CY);
            jg.writeNumber(b.cy());
            jg.writeFieldName(Data.RESERVED_KEY_BB_H);
            jg.writeNumber(b.h());
            jg.writeFieldName(Data.RESERVED_KEY_BB_W);
            jg.writeNumber(b.w());
        } else {
            jg.writeFieldName(Data.RESERVED_KEY_BB_X1);
            jg.writeNumber(bb.x1());
            jg.writeFieldName(Data.RESERVED_KEY_BB_X2);
            jg.writeNumber(bb.x2());
            jg.writeFieldName(Data.RESERVED_KEY_BB_Y1);
            jg.writeNumber(bb.y1());
            jg.writeFieldName(Data.RESERVED_KEY_BB_Y2);
            jg.writeNumber(bb.y2());
        }
        if(bb.label() != null){
            jg.writeFieldName("label");
            jg.writeString(bb.label());
        }
        if(bb.probability() != null){
            jg.writeFieldName("probability");
            jg.writeNumber(bb.probability());
        }

        jg.writeEndObject();
    }

    private void writePoint(JsonGenerator jg, Point p) throws IOException {
        jg.writeStartObject();
        jg.writeFieldName(Data.RESERVED_KEY_POINT_COORDS);
        jg.writeStartArray(p.dimensions());
        for (int i = 0; i < p.dimensions(); i++) {
            writeDouble(jg, p.get(i));
        }
        jg.writeEndArray();

        if(p.label() != null){
            jg.writeFieldName("label");
            jg.writeString(p.label());
        }
        if(p.probability() != null){
            jg.writeFieldName("probability");
            jg.writeNumber(p.probability());
        }

        jg.writeEndObject();
    }

    private void writeList(JsonGenerator jg, List<?> list, ValueType listType) throws IOException {
        int n = list.size();
        jg.writeStartArray(n);

        switch (listType) {
            case NDARRAY:
                for(NDArray arr : (List<NDArray>) list){
                    writeNDArray(jg, arr);
                }
                break;
            case STRING:
                for (String s : (List<String>) list) {             //TODO avoid unsafe cast?
                    jg.writeString(s);
                }
                break;
            case BYTES:
                for (byte[] bytes : (List<byte[]>) list) {
                    writeBytes(jg, bytes);
                }
                break;
            case BYTEBUFFER:
                for(ByteBuffer b: (List<ByteBuffer>) list) {
                    writeBytes(jg,b);
                }
                break;
            case IMAGE:
                for (Image img : (List<Image>) list) {
                    writeImage(jg, img);
                }
                break;
            case DOUBLE:
                List<Double> dList = (List<Double>) list;        //TODO checks for unsafe cast?
                for(Double d : dList){
                    writeDouble(jg, d);
                }
                break;
            case INT64:
                List<Long> lList = (List<Long>) list;
                for(Long l : lList){
                    writeLong(jg, l);
                }
                break;
            case BOOLEAN:
                List<Boolean> bList = (List<Boolean>) list;
                for (Boolean b : bList) {
                    jg.writeBoolean(b);
                }
                break;
            case DATA:
                List<Data> dataList = (List<Data>) list;
                for (Data d : dataList) {
                    writeNestedData(jg, d);
                }
                break;
            case BOUNDING_BOX:
                List<BoundingBox> bbList = (List<BoundingBox>)list;
                for(BoundingBox bb : bbList){
                    writeBB(jg, bb);
                }
                break;
            case POINT:
                List<Point> pList = (List<Point>)list;
                for(Point p : pList){
                    writePoint(jg, p);
                }
                break;
            case LIST:
                //List of lists...
                throw new IllegalStateException("Not yet implemented: Nested lists JSON serialization");
//                List<List<?>> listList = (List<List<?>>)list;
//                jg.writeStartArray(listList.size());
//                for(List<?> l : listList){
//                    ValueType vt = null;    //TODO design problem...
////                    writeList();
//                }
            default:
                throw new IllegalStateException("Not yet implemented: list type serialization for values " + listType);
        }

        jg.writeEndArray();
    }
}
