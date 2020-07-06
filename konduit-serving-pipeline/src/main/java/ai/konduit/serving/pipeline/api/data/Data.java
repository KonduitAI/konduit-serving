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
package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.impl.data.*;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.impl.serde.DataJsonDeserializer;
import ai.konduit.serving.pipeline.impl.serde.DataJsonSerializer;
import ai.konduit.serving.pipeline.util.DataUtils;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JsonSerialize(using = DataJsonSerializer.class)
@JsonDeserialize(using = DataJsonDeserializer.class)
public interface Data {

    String RESERVED_KEY_TYPE = "@type";
    String RESERVED_KEY_BYTES_BASE64 = "@BytesBase64";
    String RESERVED_KEY_BYTES_ARRAY = "@BytesArray";
    String RESERVED_KEY_IMAGE_FORMAT = "@ImageFormat";
    String RESERVED_KEY_IMAGE_DATA = "@ImageData";
    String RESERVED_KEY_NDARRAY_SHAPE = "@NDArrayShape";
    String RESERVED_KEY_NDARRAY_TYPE = "@NDArrayType";
    String RESERVED_KEY_NDARRAY_DATA_BASE64 = "@NDArrayDataBase64";
    String RESERVED_KEY_NDARRAY_DATA_ARRAY = "@NDArrayDataBase64";
    String RESERVED_KEY_METADATA = "@Metadata";
    String RESERVED_KEY_BB_X1 = "@x1";
    String RESERVED_KEY_BB_X2 = "@x2";
    String RESERVED_KEY_BB_Y1 = "@y1";
    String RESERVED_KEY_BB_Y2 = "@y2";
    String RESERVED_KEY_BB_CX = "@cx";
    String RESERVED_KEY_BB_CY = "@cy";
    String RESERVED_KEY_BB_H = "@h";
    String RESERVED_KEY_BB_W = "@w";
    String RESERVED_KEY_POINT = "@Point";
    String RESERVED_KEY_POINT_COORDS = "@Coords";
    String RESERVED_KEY_ASYNC_TRIGGER = "@AsyncTrigger";




    static List<String> reservedKeywords(){
        return Arrays.asList(RESERVED_KEY_TYPE, RESERVED_KEY_BYTES_BASE64, RESERVED_KEY_BYTES_ARRAY, RESERVED_KEY_IMAGE_FORMAT,
                RESERVED_KEY_IMAGE_DATA, RESERVED_KEY_NDARRAY_SHAPE, RESERVED_KEY_NDARRAY_TYPE, RESERVED_KEY_NDARRAY_DATA_BASE64,
                RESERVED_KEY_NDARRAY_DATA_ARRAY, RESERVED_KEY_METADATA,
                RESERVED_KEY_BB_X1, RESERVED_KEY_BB_X2, RESERVED_KEY_BB_Y1, RESERVED_KEY_BB_Y2,
                RESERVED_KEY_BB_CX, RESERVED_KEY_BB_CY, RESERVED_KEY_BB_H, RESERVED_KEY_BB_W,
                RESERVED_KEY_POINT, RESERVED_KEY_POINT_COORDS, RESERVED_KEY_ASYNC_TRIGGER);
    }

    int size();

    default String toJson(){
        try {
            return ObjectMappers.json().writeValueAsString(this);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Error serializing Data instance to JSON", e);
        }
    }

    List<String> keys();

    String key(int id);

    ValueType type(String key) throws ValueNotFoundException;

    ValueType listType(String key);

    boolean has(String key);
    default boolean hasAll(Collection<? extends String> keys){
        for(String s : keys){
            if(!has(s))
                return false;
        }
        return true;
    }

    // Getters
    Object get(String key) throws ValueNotFoundException;
    NDArray getNDArray(String key) throws ValueNotFoundException;
    String getString(String key) throws ValueNotFoundException;
    boolean getBoolean(String key) throws ValueNotFoundException;
    byte[] getBytes(String key) throws ValueNotFoundException;
    double getDouble(String key) throws ValueNotFoundException;
    Image getImage(String key) throws ValueNotFoundException;
    long getLong(String key) throws ValueNotFoundException;
    BoundingBox getBoundingBox(String key) throws ValueNotFoundException;
    Point getPoint(String key) throws ValueNotFoundException;
    List<Object> getList(String key, ValueType type);                   //TODO type
    Data getData(String key);

    List<String> getListString(String key);
    List<Long> getListInt64(String key);
    List<Boolean> getListBoolean(String key);
    List<byte[]> getListBytes(String key);
    List<Double> getListDouble(String key);

    List<Data> getListData(String key);
    List<List<?>> getListList(String key);
    List<Point> getListPoint(String key);
    List<Image> getListImage(String key);
    List<NDArray> getListNDArray(String key);
    List<BoundingBox> getListBoundingBox(String key);

    void put(String key, String data);
    void put(String key, NDArray data);
    void put(String key, byte[] data);
    void put(String key, Image data);
    void put(String key, long data);
    void put(String key, double data);
    void put(String key, boolean data);
    void put(String key, BoundingBox data);
    void put(String key, Point data);

    void putListString(String key, List<String> data);
    void putListInt64(String key, List<Long> data);
    void putListBoolean(String key, List<Boolean> data);
    void putListBytes(String key, List<byte[]> data);
    void putListDouble(String key, List<Double> data);
    void putListData(String key, List<Data> data);
    void putListList(String key, List<List<?>> data);
    void putListImage(String key, List<Image> data);
    void putListNDArray(String key, List<NDArray> data);
    void putListBoundingBox(String key, List<BoundingBox> data);
    void putListPoint(String key, List<Point> data);
    void putList(String key, List<?> data, ValueType vt);
    void put(String key, Data data);

    boolean hasMetaData();
    Data getMetaData();
    void setMetaData(Data data);
    Data clone();

    // Serialization routines
    default void save(File toFile) throws IOException {
        this.toProtoData().save(toFile);
    }


    default void write(OutputStream toStream) throws IOException {
        this.toProtoData().write(toStream);
    }

    default byte[] asBytes() {
        return this.toProtoData().asBytes();
    }

    ProtoData toProtoData();

    static Data fromJson(String json) {
        try {
            return ObjectMappers.json().readValue(json, Data.class);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Error deserializing Data from JSON", e);
        }
    }

    static Data fromBytes(byte[] input) {
        return new ProtoData(input);
    }

    static Data fromFile(File f) throws IOException {
        return new ProtoData(f);
    }

    static Data fromStream(InputStream stream) throws IOException {
        return new ProtoData(stream);
    }

    static Data singleton(@NonNull String key, @NonNull Object value){
        return JData.singleton(key, value);
    }

    static Data singletonList(@NonNull String key, @NonNull List<?> value, @NonNull ValueType valueType){
        return JData.singletonList(key, value, valueType);
    }

    static Data empty(){
        return new JData();
    }

    static String toString(Data d){
        StringBuilder sb = new StringBuilder();
        sb.append("Data(");
        sb.append(d.keys().toString());
        sb.append(")");
        return sb.toString();
    }

    static boolean equals(@NonNull Data d1, @NonNull Data d2){

        if(d1.size() != d2.size())
            return false;

        List<String> k1 = d1.keys();
        List<String> k2 = d2.keys();
        if(!k1.containsAll(k2))
            return false;

        for(String s : k1){
            if(d1.type(s) != d2.type(s))
                return false;
        }

        //All keys and types are the same at this point
        //Therefore check values
        for(String s : k1){
            ValueType vt = d1.type(s);
            switch (vt){
                default:
                    //TODO
                    throw new UnsupportedOperationException(vt + " equality not yet implemented");
                case LIST:
                    //TODO will this be robust for equality of any objects? Probably not...
                    ValueType l1Type = d1.listType(s);
                    ValueType l2Type = d2.listType(s);
                    List<?> list1 = d1.getList(s, l1Type);
                    List<?> list2 = d2.getList(s, l2Type);
                    if(!DataUtils.listEquals(list1, list2, l1Type, l2Type))
                        return false;

                    break;
                case IMAGE:
                    Png png1 = d1.getImage(s).getAs(Png.class);
                    Png png2 = d1.getImage(s).getAs(Png.class);

                    byte[] pngBytes1 = png1.getBytes();
                    byte[] pngBytes2 = png2.getBytes();

                    if(!Arrays.equals(pngBytes1, pngBytes2))
                        return false;
                    break;
                case NDARRAY:
                    //TODO this is inefficient - but robust...
                    NDArray a1 = d1.getNDArray(s);
                    NDArray a2 = d2.getNDArray(s);
                    SerializedNDArray sa1 = a1.getAs(SerializedNDArray.class);
                    SerializedNDArray sa2 = a2.getAs(SerializedNDArray.class);
                    if(!sa1.equals(sa2))
                        return false;
                    break;
                case STRING:
                    if(!d1.getString(s).equals(d2.getString(s)))
                        return false;
                    break;
                case BYTES:
                    byte[] b1 = d1.getBytes(s);
                    byte[] b2 = d2.getBytes(s);
                    if(!Arrays.equals(b1, b2))
                        return false;
                    break;
                case DOUBLE:
                    double dbl1 = d1.getDouble(s);
                    double dbl2 = d2.getDouble(s);
                    if(dbl1 != dbl2 && !(Double.isNaN(dbl1) && Double.isNaN(dbl2))){        //Both equal, or both NaN
                        return false;
                    }
                    break;
                case INT64:
                    long l1 = d1.getLong(s);
                    long l2 = d2.getLong(s);
                    if(l1 != l2)
                        return false;
                    break;
                case BOOLEAN:
                    boolean bool1 = d1.getBoolean(s);
                    boolean bool2 = d2.getBoolean(s);
                    if(bool1 != bool2)
                        return false;
                    break;
                case DATA:
                    Data d1a = d1.getData(s);
                    Data d2a = d2.getData(s);
                    if(!equals(d1a, d2a))
                        return false;
                    break;
                case BOUNDING_BOX:
                    BoundingBox bb1 = d1.getBoundingBox(s);
                    BoundingBox bb2 = d2.getBoundingBox(s);
                    if(!BoundingBox.equals(bb1, bb2))
                        return false;
                    break;
                case POINT:
                    Point p1 = d1.getPoint(s);
                    Point p2 = d2.getPoint(s);
                    if(!Point.equals(p1, p2))
                        return false;
                    break;
            }
        }

        return true;
    }



    static void assertNotReservedKey(@NonNull String s){
        for(String kwd : reservedKeywords()){
            if(kwd.equalsIgnoreCase(s)){
                throw new IllegalStateException("Cannot use key \"" + kwd + "\" in a Data instance: This key is reserved" +
                        " for internal use only");
            }
        }
    }


    default void copyFrom(@NonNull String key, @NonNull Data from){
        Preconditions.checkState(from.has(key), "Key %s does not exist in provided Data instance");
        ValueType vt = from.type(key);
        //TODO is there a cleaner way?
        switch (vt){
            case NDARRAY:
                put(key, from.getNDArray(key));
                return;
            case STRING:
                put(key, from.getString(key));
                return;
            case BYTES:
                put(key, from.getBytes(key));
                return;
            case IMAGE:
                put(key, from.getImage(key));
                return;
            case DOUBLE:
                put(key, from.getDouble(key));
                return;
            case INT64:
                put(key, from.getLong(key));
                return;
            case BOOLEAN:
                put(key, from.getBoolean(key));
                return;
            case POINT:
                put(key, from.getPoint(key));
                return;
            case DATA:
                put(key, from.getData(key));
                return;
            case BOUNDING_BOX:
                put(key, from.getBoundingBox(key));
                return;
            case LIST:
                ValueType vtList = from.listType(key);
                List<?> l = from.getList(key, vtList);
                switch (vtList){
                    case NDARRAY:
                        putListNDArray(key, (List<NDArray>)l);
                        break;
                    case STRING:
                        putListString(key, (List<String>)l);
                        break;
                    case BYTES:
                        putListBytes(key, (List<byte[]>)l);
                        break;
                    case IMAGE:
                        putListImage(key, (List<Image>)l);
                        break;
                    case DOUBLE:
                        putListDouble(key, (List<Double>)l);
                        break;
                    case INT64:
                        putListInt64(key, (List<Long>)l);
                        break;
                    case BOOLEAN:
                        putListBoolean(key, (List<Boolean>)l);
                        break;
                    case BOUNDING_BOX:
                        putListBoundingBox(key, (List<BoundingBox>)l);
                        break;
                    case POINT:
                        putListPoint(key, (List<Point>)l);
                        break;
                    case DATA:
                        putListData(key, (List<Data>)l);
                        break;
                    case LIST:
                        putList(key, l, ValueType.LIST);
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported: " + vt);
                }
                break;
            default:
                throw new UnsupportedOperationException("Not supported: " + vt);
        }
    }

    /**
     * Merge all of the specified Data instance values into this Data instance
     * @param allowOverwrite If true: allow duplicate keys to overwrite the keys in this Data instance
     * @param datas          The Data instances to merge into this one
     */
    default void merge(boolean allowOverwrite, Data... datas){
        for(Data d : datas){
            for(String s : d.keys()){
                //TODO we should check this BEFORE doing merging
                if(has(s) && !allowOverwrite)
                    throw new IllegalStateException("Error during merging: Data instance already has key \"" + s + "\" and allowOverwrite is false");

                this.copyFrom(s, d);
            }
        }
    }
}
