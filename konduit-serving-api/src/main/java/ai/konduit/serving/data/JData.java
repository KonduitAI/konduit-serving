/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
package ai.konduit.serving.data;

import ai.konduit.serving.data.wrappers.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.*;
import java.util.*;

@Slf4j
public class JData implements Data {

    private Map<String, Value> dataMap = new LinkedHashMap<>();
    private Data metaData;

    public Map<String, Value> getDataMap() {
        return dataMap;
    }

    @Override
    public String toJson() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> keys() {
        Set<String> keys = dataMap.keySet();
        List<String> retVal = new ArrayList<>();
        retVal.addAll(keys);
        return retVal;
    }

    @Override
    public String key(int id) {
        String retVal = null;
        Iterator<Map.Entry<String,Value>> iterator = dataMap.entrySet().iterator();
        for (int i = 0; i < id; ++i) {
            retVal = iterator.next().getKey();
        }
        return retVal;
    }

    private Value valueIfFound(String key, ValueType type) {
        Value data = dataMap.get(key);
        if (data == null)
            throw new ValueNotFoundException(String.format("Value not found for key %s", key));
        if (data.type() != type)
            throw new IllegalStateException(String.format("Value has wrong type for key %s", key));
        return data;
    }

    @Override
    public ValueType type(String key) {
        Value data = dataMap.get(key);
        if (data == null)
            throw new ValueNotFoundException(String.format("Value not found for key %s", key));
        return data.type();
    }

    @Override
    public ValueType listType(String key) {
        return null;
    }

    @Override
    public INDArray getNDArray(String key) {
        Value<INDArray> data = valueIfFound(key, ValueType.NDARRAY);
        return data.get();
    }

    @Override
    public String getString(String key) {
        Value<String> data = valueIfFound(key, ValueType.STRING);
        return data.get();
    }

    @Override
    public boolean getBoolean(String key) {
        Value<Boolean> data = valueIfFound(key, ValueType.BOOLEAN);
        return data.get();
    }

    @Override
    public byte[] getBytes(String key) {
        Value<byte[]> data = valueIfFound(key, ValueType.BYTES);
        return data.get();
    }

    @Override
    public double getDouble(String key) {
        Value<Double> data = valueIfFound(key, ValueType.DOUBLE);
        return data.get();
    }

    @Override
    public Image getImage(String key) {
        Value<Image> data = valueIfFound(key, ValueType.IMAGE);
        return data.get();
    }

    @Override
    public long getLong(String key) {
        Value<Long> data = valueIfFound(key, ValueType.INT64);
        return data.get();
    }

    @Override
    public List<ValueType> getList(String key, DataType type) {
        return null;
    }

    @Override
    public Data getData(String key) {
        Data data = (Data)dataMap.get(key);
        return data;
    }

    @Override
    public void put(String key, String data) {
        dataMap.put(key, new StringValue(data));
    }

    @Override
    public void put(String key, INDArray data) {
        dataMap.put(key, new INDArrayValue(data));
    }

    @Override
    public void put(String key, byte[] data) {
        dataMap.put(key, new BytesValue(data));
    }

    @Override
    public void put(String key, Image data) {
        dataMap.put(key, new ImageValue(data));
    }

    @Override
    public void put(String key, long data) {
        dataMap.put(key, new IntValue(data));
    }

    @Override
    public void put(String key, double data) {
        dataMap.put(key, new DoubleValue(data));
    }

    @Override
    public void put(String key, boolean data) {
        dataMap.put(key, new BooleanValue(data));
    }

    @Override
    public void put(String key, Data data) {
        // TODO: must avoid cast and redesign method
        this.dataMap.putAll(((JData)data).getDataMap());
    }

    @Override
    public boolean hasMetaData() {
        return metaData != null;
    }

    @Override
    public Data getMetaData() {
        return metaData;
    }

    @Override
    public void setMetaData(Data data) {
        this.metaData = data;
    }

    @Override
    public void save(File toFile) {

        throw new UnsupportedOperationException();
    }

    public static Data fromFile(File fromFile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(OutputStream toStream) {
        throw new UnsupportedOperationException();
    }

    public byte[] asBytes() {
        throw new UnsupportedOperationException();
    }

    private static Data instance = null;

    public static Data empty() {
        Data retVal = new JData();
        return retVal;
    }

    static Data makeData(String key, Object data) {
        if (instance == null) {
            instance = new JData();
        }
        if (data instanceof String) {
            instance.put(key, (String)data);
        }
        else if (data instanceof INDArray) {
            instance.put(key, (INDArray)data);
        }
        else if (data instanceof Boolean) {
            instance.put(key, (Boolean)data);
        }
        else if (data instanceof Long) {
            instance.put(key, (Long)data);
        }
        else if (data instanceof Double) {
            instance.put(key, (Double)data);
        }
        else if (data instanceof Image) {
            instance.put(key, (Image)data);
        }
        else if (data instanceof Byte[]) {
            byte[] input = ArrayUtils.toPrimitive((Byte[])data);
            instance.put(key, input);
        }
        else if (data instanceof byte[]) {
            instance.put(key, (byte[]) data);
        }
        else if (data instanceof Data) {
            instance.put(key, (Data)data);
        }
        else {
            throw new IllegalStateException("Trying to put data of not supported type");
        }
        return instance;
    }

    public static class DataBuilder {
        private JData instance;

        public DataBuilder() {
            instance = new JData();
        }

        public DataBuilder add(String key, String data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, Boolean data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, byte[] data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, Double data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, Image data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, INDArray data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, Long data) {
            instance.put(key, data);
            return this;
        }

        public JData builld() {
            return instance;
        }
    }
}
