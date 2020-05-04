/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.api;

import ai.konduit.serving.pipeline.impl.data.*;
import lombok.NonNull;

import java.io.*;
import java.util.Collection;
import java.util.List;

public interface Data {

    int size();

    String toJson();

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
    List<Object> getList(String key);                   //TODO type
    Data getData(String key);

    void put(String key, String data);
    void put(String key, NDArray data);
    void put(String key, byte[] data);
    void put(String key, Image data);
    void put(String key, long data);
    void put(String key, double data);
    void put(String key, boolean data);
    void put(String key, List<?> data);
    //void put(String key, Data data);

    void put(String key, Data data);

    boolean hasMetaData();
    Data getMetaData();
    void setMetaData(Data data);

    // Serialization routines
    void save(File toFile) throws IOException;


    void write(OutputStream toStream) throws IOException;

    static Data fromJson(String key) {
        return null;
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


    static Data singleton(@NonNull String key, @NonNull Object value){
        return JData.singleton(key, value);
    }
}
