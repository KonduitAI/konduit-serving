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
package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.impl.data.wrappers.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
    public int size() {
        return dataMap.size();
    }

    @Override
    public String toJson() {
        StringBuilder jsonString = new StringBuilder();
        try {
            Map<String, generated.Data.DataScheme> newItemsMap = javaMapToPbMap();
            for (String key : newItemsMap.keySet()) {
                jsonString.append(JsonFormat.printer().print(newItemsMap.get(key)));
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed toJson conversion",e);
        }
        return jsonString.toString();
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
    public boolean has(String key) {
        return dataMap.containsKey(key);
    }

    @Override
    public Object get(String key) throws ValueNotFoundException {
        if(!dataMap.containsKey(key))
            throw new ValueNotFoundException("Value not found for key " + key);
        return dataMap.get(key).get();
    }

    @Override
    public NDArray getNDArray(String key) {
        Value<NDArray> data = valueIfFound(key, ValueType.NDARRAY);
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
    public List<Object> getList(String key, ValueType type) {
        Value<List<Object>> data = valueIfFound(key, ValueType.LIST);
        return data.get();
    }

    @Override
    public List<Object> getList(String key) {
        Value<List<Object>> data = valueIfFound(key, ValueType.LIST);
        return data.get();
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
    public void put(String key, NDArray data) {
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
    public void putListString(String key, List<String> data) {
        dataMap.put(key, new ListValue(data));
    }

    @Override
    public void putListInt64(String key, List<Long> data) {
        dataMap.put(key, new ListValue(data));
    }

    @Override
    public void putListBoolean(String key, List<Boolean> data) {
        dataMap.put(key, new ListValue(data));
    }

    @Override
    public void putListDouble(String key, List<Double> data) {
        dataMap.put(key, new ListValue(data));
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
    public void save(File toFile) throws IOException {
        write(new FileOutputStream(toFile));
    }

    public static Data fromFile(File fromFile) throws IOException {
        generated.Data.DataMap.Builder builder = generated.Data.DataMap.newBuilder().mergeFrom(new FileInputStream(fromFile));
        generated.Data.DataMap dataMap = builder.build();
        JData retData = new JData();
        Map<String, generated.Data.DataScheme> schemeMap = dataMap.getMapItemsMap();
        Iterator<Map.Entry<String, generated.Data.DataScheme>> iterator =
                schemeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, generated.Data.DataScheme> entry = iterator.next();
            generated.Data.DataScheme item = entry.getValue();
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.STRING.ordinal()) {
                retData.put(entry.getKey(), item.getSValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.BOOLEAN.ordinal()) {
                retData.put(entry.getKey(), item.getBoolValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.INT64.ordinal()) {
                retData.put(entry.getKey(), item.getIValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.DOUBLE.ordinal()) {

            }
        }
        return retData;
    }

    private Map<String, generated.Data.DataScheme> javaMapToPbMap() {
        Map<String, generated.Data.DataScheme> newItemsMap = new HashMap<>();
        Iterator<Map.Entry<String, Value>> iterator = dataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            generated.Data.DataScheme item = null;

            if (value.type().equals(ValueType.STRING)) {
                item = generated.Data.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.BOOLEAN)) {
                item = generated.Data.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.INT64)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            newItemsMap.put(nextItem.getKey(), item);
        }
        return newItemsMap;
    }

    @Override
    public void write(OutputStream toStream) throws IOException {

        Map<String, generated.Data.DataScheme> newItemsMap = javaMapToPbMap();

        generated.Data.DataMap pbDataMap = generated.Data.DataMap.newBuilder().
                putAllMapItems(newItemsMap).
                build();
        pbDataMap.writeTo(toStream);
    }

    public byte[] asBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(baos);
        } catch (IOException e) {
            log.error("Failed write to ByteArrayOutputStream", e);
        }
        return baos.toByteArray();
    }

    public static Data empty() {
        Data retVal = new JData();
        return retVal;
    }
    public static Data singleton(@NonNull String key, @NonNull Object data) {
        Data instance = new JData();
        if (data instanceof String) {
            instance.put(key, (String)data);
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
        } else if(data instanceof NDArray){
            instance.put(key, (NDArray)data);
        }
//        else if (data instanceof Object) {
//            instance.put(key, (Object)data);
//        }
        else {
            throw new IllegalStateException("Trying to put data of not supported type: " + data.getClass());
        }
        return instance;
    }

    public static Data singletonList(@NonNull String key, @NonNull List<?> data,
                                     @NonNull ValueType valueType) {
        Data instance = new JData();
        if (valueType.equals(ValueType.STRING)) {
            instance.putListString(key, (List<String>)data);
        }
        else if (valueType.equals(ValueType.BOOLEAN)) {
            instance.putListBoolean(key, (List<Boolean>) data);
        }
        else if (valueType.equals(ValueType.DOUBLE)) {
            instance.putListDouble(key, (List<Double>) data);
        }
        else if (valueType.equals(ValueType.INT64)) {
            instance.putListInt64(key, (List<Long>) data);
        }
        else {
            throw new IllegalStateException("Trying to put list data of not supported type: " + data.getClass());
        }
        return instance;
    }

    public static DataBuilder builder(){
        return new DataBuilder();
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

        public DataBuilder add(String key, Long data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, NDArray data){
            instance.put(key, data);
            return this;
        }

        public DataBuilder addListString(String key, List<String> data) {
            instance.putListString(key, data);
            return this;
        }

        public DataBuilder addListInt64(String key, List<Long> data) {
            instance.putListInt64(key, data);
            return this;
        }

        public DataBuilder addListBoolean(String key, List<Boolean> data) {
            instance.putListBoolean(key, data);
            return this;
        }

        public DataBuilder addListDouble(String key, List<Double> data) {
            instance.putListDouble(key, data);
            return this;
        }

        public JData build() {
            return instance;
        }
    }
}
