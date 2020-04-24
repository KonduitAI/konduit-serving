package ai.konduit.serving.data;

import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.json.JsonValue;
import java.io.File;
import java.io.OutputStream;
import java.util.List;

public class JData implements Data {
    @Override
    public String toJson() {
        return null;
    }

    @Override
    public List<String> keys() {
        return null;
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
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public List<JsonValue.ValueType> getList(String key, DataType type) {
        return null;
    }

    @Override
    public Data getData(String key) {
        return null;
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
}
