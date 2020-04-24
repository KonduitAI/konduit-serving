package ai.konduit.serving.data;

import ai.konduit.serving.util.ObjectMappers;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.json.JsonValue;
import java.io.File;
import java.io.OutputStream;
import java.util.*;

public class JData implements Data {

    private Map<String, Object> dataMap = new HashMap<>();

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
        return null;
    }

    @Override
    public DataType type(String key) {
        return null;
    }

    @Override
    public DataType listType(String key) {
        return null;
    }

    @Override
    public INDArray getArray(String key) {
        INDArray data = (INDArray) dataMap.get(key);
        return data;
    }

    @Override
    public String getString(String key) {
        String data = (String)dataMap.get(key);
        return data;
    }

    @Override
    public boolean getBoolean(String key) {
        Boolean data = (Boolean) dataMap.get(key);
        return data;
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
        dataMap.put(key, data);
    }

    @Override
    public void put(String key, INDArray data) {
        dataMap.put(key, data);
    }

    @Override
    public void put(String key, byte[] data) {
        dataMap.put(key, data);
    }

    @Override
    public void put(String key, Data data) {
        dataMap.put(key, data);
    }

    @Override
    public boolean hasMetaData() {
        return false;
    }

    @Override
    public Data getMetaData() {
        return null;
    }

    @Override
    public void setMetaData(Data data) {

    }

    @Override
    public void save(File toFile) {

    }

    @Override
    public void write(OutputStream toStream) {

    }

    @Override
    public byte[] asBytes() {
        return new byte[0];
    }

    private static Data instance = null;

    static Data makeData(String key, Object data) {
        if (instance == null) {
            instance = new JData();
        }
        if (data instanceof String) {
            instance.put(key, (String)data);
        }
        else if (data instanceof INDArray) {
            instance.put(key, (INDArray) data);
        }
        else {
            throw new IllegalStateException("Trying to put data of not supported type");
        }
        return instance;
    }
}
