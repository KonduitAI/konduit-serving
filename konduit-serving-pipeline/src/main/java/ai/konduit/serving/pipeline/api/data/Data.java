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
import ai.konduit.serving.pipeline.impl.serde.DataJsonDeserializer;
import ai.konduit.serving.pipeline.impl.serde.DataJsonSerializer;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.NonNull;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JsonSerialize(using = DataJsonSerializer.class)
@JsonDeserialize(using = DataJsonDeserializer.class)
public interface Data {

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
    List<Object> getList(String key, ValueType type);                   //TODO type
    Data getData(String key);

    void put(String key, String data);
    void put(String key, NDArray data);
    void put(String key, byte[] data);
    void put(String key, Image data);
    void put(String key, long data);
    void put(String key, double data);
    void put(String key, boolean data);
    //void put(String key, Data data);

    void put(String key, Data data);

    boolean hasMetaData();
    Data getMetaData();
    void setMetaData(Data data);

    // Serialization routines
    default void save(File toFile){
        throw new UnsupportedOperationException();
    }


    default void write(OutputStream toStream){
        throw new UnsupportedOperationException();
    }

    static Data fromJson(String json) {
        try {
            return ObjectMappers.json().readValue(json, Data.class);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Error deserializing Data from JSON", e);
        }
    }

    /*static Data fromFile(File file) {
        return null;
    }*/

    static Data fromStream(InputStream fromStream) {
        return null;
    }

    static Data fromBytes(InputStream fromStream) {
        return null;
    }

    static Data fromFile(File f){
        throw new UnsupportedOperationException("Not yet implemented");
    }


    static Data singleton(@NonNull String key, @NonNull Object value){
        return JData.singleton(key, value);
    }

    static Data empty(){
        return new JData();
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
                case NDARRAY:
                case LIST:
                case IMAGE:
                default:
                    //TODO
                    throw new UnsupportedOperationException(vt + " equality not yet implemented");
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

            }
        }

        return true;
    }
}
