package ai.konduit.serving.data;

import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.json.JsonValue;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface Data {

    String toJson();

    List<String> keys();

    String key(int id);

    DataType type(String key);

    DataType listType(String key);

    // Getters
    INDArray getArray(String key);
    String getString(String key);
    boolean getBoolean(String key);
    List<JsonValue.ValueType> getList(String key, DataType type);
    Data getData(String key);

    void put(String key, String data);
    void put(String key, INDArray data);
    void put(String key, byte[] data);
    void put(String key, Data data);

    boolean hasMetaData();
    Data getMetaData();
    void setMetaData(Data data);

    // Serialization routines
    void save(File toFile);
    void write(OutputStream toStream);
    byte[] asBytes();

    static Data fromJson(String key) {
        return null;
    }

    static Data fromFile(File file) {
        return null;
    }

    static Data fromStream(InputStream fromStream) {
        return null;
    }

    static Data fromBytes(InputStream fromStream) {
        return null;
    }

}
