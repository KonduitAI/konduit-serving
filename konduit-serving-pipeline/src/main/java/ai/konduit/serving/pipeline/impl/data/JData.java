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

import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.wrappers.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Slf4j
public class JData implements Data {

    private Map<String, Value> dataMap = new LinkedHashMap<>();
    private Data metaData;

    private static final String VALUE_NOT_FOUND_TEXT = "Value not found for key \"%s\"";
    private static final String VALUE_HAS_WRONG_TYPE_TEXT = "Value has wrong type for key \"%s\": requested type %s, actual type %s";

    public Map<String, Value> getDataMap() {
        return dataMap;
    }

    @Override
    public int size() {
        return dataMap.size();
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
            throw new ValueNotFoundException(String.format(VALUE_NOT_FOUND_TEXT, key));
        if (data.type() != type)
            throw new IllegalStateException(String.format(VALUE_HAS_WRONG_TYPE_TEXT, key, type, data.type()));
        return data;
    }

    private <T> List<T> listIfFound(String key, ValueType listType){
        Value lValue = dataMap.get(key);
        if(lValue == null)
            throw new ValueNotFoundException(String.format(VALUE_NOT_FOUND_TEXT, key));
        if(lValue.type() != ValueType.LIST)
            throw new IllegalStateException(String.format(VALUE_HAS_WRONG_TYPE_TEXT, key, ValueType.LIST, lValue.type()));

        //TODO Check list type
        return (List<T>) lValue.get();
    }

    @Override
    public ValueType type(String key) {
        Value data = dataMap.get(key);
        if (data == null)
            throw new ValueNotFoundException(String.format(VALUE_NOT_FOUND_TEXT, key));
        return data.type();
    }

    @Override
    public ValueType listType(String key) {
        Value data = dataMap.get(key);
        if (data == null || !(data instanceof ListValue))
            throw new ValueNotFoundException(String.format(VALUE_NOT_FOUND_TEXT, key));
        return ((ListValue)data).elementType();
    }

    @Override
    public boolean has(String key) {
        return dataMap.containsKey(key);
    }

    @Override
    public Object get(String key) throws ValueNotFoundException {
        if(!dataMap.containsKey(key))
            throw new ValueNotFoundException(String.format(VALUE_NOT_FOUND_TEXT, key));
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
    public BoundingBox getBoundingBox(String key) throws ValueNotFoundException {
        Value<BoundingBox> data = valueIfFound(key, ValueType.BOUNDING_BOX);
        return data.get();
    }

    @Override
    public Point getPoint(String key) throws ValueNotFoundException {
        Value<Point> data = valueIfFound(key, ValueType.POINT);
        return data.get();
    }

    @Override
    public List<Object> getList(String key, ValueType type) {
        Value<List<Object>> data = valueIfFound(key, ValueType.LIST);
        return data.get();
    }

    @Override
    public Data getData(String key) {
        Value<Data> data = valueIfFound(key, ValueType.DATA);
        return data.get();
    }

    @Override
    public List<String> getListString(String key) {
        return listIfFound(key, ValueType.STRING);
    }

    @Override
    public List<Long> getListInt64(String key) {
        return listIfFound(key, ValueType.INT64);
    }

    @Override
    public List<Boolean> getListBoolean(String key) {
        return listIfFound(key, ValueType.BOOLEAN);
    }

    @Override
    public List<byte[]> getListBytes(String key) {
        return listIfFound(key, ValueType.BYTES);
    }

    @Override
    public List<Double> getListDouble(String key) {
        return listIfFound(key, ValueType.DOUBLE);
    }

    @Override
    public List<Data> getListData(String key) {
        return listIfFound(key, ValueType.DATA);
    }

    @Override
    public List<List<?>> getListList(String key) {
        return listIfFound(key, ValueType.LIST);
    }
  
    @Override
    public List<Point> getListPoint(String key) {
        return listIfFound(key, ValueType.POINT);
    }
  
    @Override
    public List<Image> getListImage(String key) {
        return listIfFound(key, ValueType.IMAGE);
    }

    @Override
    public List<NDArray> getListNDArray(String key) {
        return listIfFound(key, ValueType.NDARRAY);
    }

    @Override
    public List<BoundingBox> getListBoundingBox(String key) {
        return listIfFound(key, ValueType.BOUNDING_BOX);
    }

    @Override
    public void put(String key, String data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new StringValue(data));
    }

    @Override
    public void put(String key, NDArray data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new NDArrayValue(data));
    }

    @Override
    public void put(String key, byte[] data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new BytesValue(data));
    }

    @Override
    public void put(String key, Image data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ImageValue(data));
    }

    @Override
    public void put(String key, long data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new IntValue(data));
    }

    @Override
    public void put(String key, double data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new DoubleValue(data));
    }

    @Override
    public void put(String key, boolean data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new BooleanValue(data));
    }

    @Override
    public void put(String key, BoundingBox data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new BBoxValue(data));
    }

    @Override
    public void put(String key, Point data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new PointValue(data));
    }

    @Override
    public void put(String key, Data data) {
        if (!StringUtils.equals(key, Data.RESERVED_KEY_METADATA))
            Data.assertNotReservedKey(key);
        this.dataMap.put(key, new DataValue(data));
    }

    @Override
    public void putListString(String key, List<String> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.STRING));
    }

    @Override
    public void putListInt64(String key, List<Long> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.INT64));
    }

    @Override
    public void putListBoolean(String key, List<Boolean> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.BOOLEAN));
    }

    @Override
    public void putListBytes(String key, List<byte[]> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.BYTES));
    }

    @Override
    public void putListDouble(String key, List<Double> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.DOUBLE));
    }

    @Override
    public void putListData(String key, List<Data> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.DATA));
    }

    @Override
    public void putListList(String key, List<List<?>> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.LIST));
    }

    @Override
    public void putListImage(String key, List<Image> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.IMAGE));
    }

    @Override
    public void putListNDArray(String key, List<NDArray> data) {
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, ValueType.NDARRAY));
    }

    @Override
    public void putListBoundingBox(String key, List<BoundingBox> data) {
        dataMap.put(key, new ListValue(data, ValueType.BOUNDING_BOX));
    }

    @Override
    public void putListPoint(String key, List<Point> data) {
        dataMap.put(key, new ListValue(data, ValueType.POINT));
    }

    public void putList(String key, List<?> data, ValueType vt){
        Data.assertNotReservedKey(key);
        dataMap.put(key, new ListValue(data, vt));
    }

    @Override
    public boolean hasMetaData() {
        return this.metaData != null;
    }

    @Override
    public Data getMetaData() {
        return this.metaData;
    }

    @Override
    public void setMetaData(Data data) {
        this.metaData = data;
    }

    @Override
    public ProtoData toProtoData() {
        return new ProtoData(this);
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Data))
            return false;
        return Data.equals(this, (Data)o);
    }

    @Override
    public String toString(){
        return Data.toString(this);
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
        else if (data instanceof  Integer) {
            instance.put(key, ((Integer) data).longValue());
        }
        else if (data instanceof Long) {
            instance.put(key, (Long)data);
        }
        else if (data instanceof Float) {
            instance.put(key, ((Float) data).doubleValue());
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
        else if(data instanceof NDArray){
            instance.put(key, (NDArray)data);
        }
        else if (data instanceof Image) {
            instance.put(key, (Image)data);
        } else if(data instanceof BoundingBox){
            instance.put(key, (BoundingBox)data);
        } else if(data instanceof Point){
            instance.put(key, (Point)data);
        } else if (data instanceof NDArray) {
            instance.put(key, (NDArray) data);
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
        JData instance = new JData();
        if (valueType == ValueType.STRING) {
            instance.putListString(key, (List<String>) data);
        } else if (valueType == ValueType.BOOLEAN) {
            instance.putListBoolean(key, (List<Boolean>) data);
        } else if (valueType == ValueType.DOUBLE) {
            instance.putListDouble(key, (List<Double>) data);
        } else if (valueType == ValueType.INT64) {
            instance.putListInt64(key, (List<Long>) data);
        } else if (valueType == ValueType.IMAGE) {
            instance.putListImage(key, (List<Image>) data);
        } else if (valueType == ValueType.NDARRAY) {
            instance.putListNDArray(key, (List<NDArray>) data);
        } else if (valueType == ValueType.DATA) {
            instance.putListData(key, (List<Data>) data);
        } else if (valueType == ValueType.BYTES) {
            instance.putListBytes(key, (List<byte[]>) data);
        } else if(valueType == ValueType.BOUNDING_BOX){
            instance.putListBoundingBox(key, (List<BoundingBox>)data);
        } else if(valueType == ValueType.POINT){
            instance.putListPoint(key, (List<Point>)data);
        } else if (valueType == ValueType.LIST) {
            //TODO don't use JData - use Data interface
            instance.putList(key, data, valueType);
        } else {
            throw new IllegalStateException("Trying to put list data of not supported type: " + valueType);
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
        
        public DataBuilder add(String key, BoundingBox data){
            instance.put(key, data);
            return this;
        }

        public DataBuilder add(String key, Point data){
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

        public DataBuilder addListImage(String key, List<Image> data) {
            instance.putListImage(key, data);
            return this;
        }

        public DataBuilder addListData(String key, List<Data> data) {
            instance.putListData(key, data);
            return this;
        }

        public DataBuilder addListNDArray(String key, List<NDArray> data) {
            instance.putListNDArray(key, data);
            return this;
        }

        public DataBuilder addListBoundingBox(String key, List<BoundingBox> data) {
            instance.putListBoundingBox(key, data);
            return this;
        }

        public DataBuilder addListPoint(String key, List<Point> data) {
            instance.putListPoint(key, data);
            return this;
        }

        public JData build() {
            return instance;
        }
    }

    @Override
    public Data clone(){
        Data ret = empty();
        ret.merge(true, this);
        return ret;
    }
}
