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
import ai.konduit.serving.util.ObjectMappers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.datavec.image.data.Image;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.json.JsonValue;
import java.io.*;
import java.util.*;

@Slf4j
public class JData implements Data {

    private Map<String, Value> dataMap = new HashMap<>();
    private Data metaData;

    @Override
    public Map<String, Value> getDataMap() {
        return dataMap;
    }

    @Override
    public String toJson() {
        return ObjectMappers.toJson(dataMap);
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

    private Value valueIfFound(String key) throws ValueNotFoundException {
        Value data = dataMap.get(key);
        if (data == null)
            throw new ValueNotFoundException();
        return data;
    }

    @Override
    public ValueType type(String key) throws ValueNotFoundException {
        Value<?> data = valueIfFound(key);
        return data.type();
    }

    @Override
    public ValueType listType(String key) {
        return null;
    }

    @Override
    public INDArray getNDArray(String key) throws ValueNotFoundException {
        Value<INDArray> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public String getString(String key) throws ValueNotFoundException {
        Value<String> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public boolean getBoolean(String key) throws ValueNotFoundException {
        Value<Boolean> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public byte[] getBytes(String key) throws ValueNotFoundException {
        Value<Byte[]> data = valueIfFound(key);
        return ArrayUtils.toPrimitive(data.get());
    }

    @Override
    public double getDouble(String key) throws ValueNotFoundException {
        Value<Double> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public Image getImage(String key) throws ValueNotFoundException {
        Value<Image> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public long getLong(String key) throws ValueNotFoundException {
        Value<Long> data = valueIfFound(key);
        return data.get();
    }

    @Override
    public List<JsonValue.ValueType> getList(String key, DataType type) {
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
        this.dataMap.putAll(data.getDataMap());
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

        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(toFile))) {
            write(stream);
        } catch (Exception e) {
            log.error("Error saving Data object to file",e);
        }
    }

    public static Data fromFile(File fromFile) {
        Data retVal = JData.empty();
        try (ObjectInputStream osi = new ObjectInputStream(new FileInputStream(fromFile))) {
            retVal = (JData)osi.readObject();

        } catch (Exception e) {
            log.error("Error saving Data object to file",e);
        }
        return retVal;
    }

    @Override
    public void write(OutputStream toStream) {
        try (ObjectOutputStream oos = new ObjectOutputStream(toStream)) {
            oos.writeObject(this.dataMap);
            if (hasMetaData())
                oos.writeObject(metaData.getDataMap());
        } catch (IOException e) {
            log.error("Error serializing Data", e);
        }
    }

    @Override
    public byte[] asBytes() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(this.dataMap);
            if (hasMetaData())
                out.writeObject(metaData.getDataMap());
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to convert Data to bytes[]", e);
        }
        return null;
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

        public DataBuilder withStringData(String key, String data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withBooleanData(String key, Boolean data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withBytesData(String key, byte[] data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withDoubleData(String key, Double data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withImageData(String key, Image data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withNDArrayData(String key, INDArray data) {
            instance.put(key, data);
            return this;
        }

        public DataBuilder withIntValue(String key, Long data) {
            instance.put(key, data);
            return this;
        }

        public JData builld() {
            return instance;
        }
    }
}
