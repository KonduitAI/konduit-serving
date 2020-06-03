package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.data.JData;
import org.bytedeco.javacpp.BytePointer;
import org.eclipse.python4j.*;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.List;

public class PyData extends PythonType<Data> {

    public static final PyData INSTANCE = new PyData();
    public PyData(){
        super("Data", Data.class);
    }
    @Override
    public Data toJava(PythonObject pythonObject) throws PythonException{
        JData data = new JData();
        try (PythonGC gc = PythonGC.watch()){
            PythonObject pyKeysList = Python.list(pythonObject.attr("keys").call());
            List keysList = PythonTypes.LIST.toJava(pyKeysList);
            for (Object key: keysList){
                if (!(key instanceof String)){
                    throw new PythonException("String key expected."); // already checked in python
                }
                String strKey = (String)key;
                PythonObject val = pythonObject.get(strKey);
                if (Python.isinstance(val, Python.strType())){
                    String jVal = PythonTypes.STR.toJava(val);
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.intType())){
                    long jVal = PythonTypes.INT.toJava(val);
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.floatType())){
                    double jVal = PythonTypes.FLOAT.toJava(val);
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.boolType())){
                    boolean jVal = PythonTypes.BOOL.toJava(val);
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.memoryviewType())){
                    BytePointer bp = PythonTypes.MEMORYVIEW.toJava(val);
                    byte[] jVal = bp.getStringBytes();
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.importModule("numpy").attr("ndarray"))){
                    INDArray arr = NumpyArray.INSTANCE.toJava(val);
                    NDArray jVal = NDArray.create(arr);
                    data.put(strKey, jVal);
                }
                else if (Python.isinstance(val, Python.importModule("PIL.Image").attr("Image"))){
                    // TODO
                    throw new PythonException("Image not supprted yet.");
                }
                else if (Python.isinstance(val, Python.listType())){
                    long size = Python.len(val).toLong();
                    if (size == 0){
                        throw new PythonException("Cannot infer type from empty list.");
                    }
                    PythonObject item0 = val.get(0);
                    if (Python.isinstance(item0, Python.strType())){
                        List<String> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(PythonTypes.STR.toJava(val.get(i)));
                        }
                        data.putListString(strKey, jVal);
                    }else if (Python.isinstance(item0, Python.intType())){
                        List<Long> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(PythonTypes.INT.toJava(val.get(i)));
                        }
                        data.putListInt64(strKey, jVal);
                    }else if (Python.isinstance(item0, Python.floatType())){
                        List<Double> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(PythonTypes.FLOAT.toJava(val.get(i)));
                        }
                        data.putListDouble(strKey, jVal);
                    }else if (Python.isinstance(item0, Python.boolType())){
                        List<Boolean> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(PythonTypes.BOOL.toJava(val.get(i)));
                        }
                        data.putListBoolean(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.memoryviewType())){
                        List<byte[]> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(PythonTypes.MEMORYVIEW.toJava(val.get(i)).getStringBytes());
                        }
                        data.putListBytes(strKey, jVal);
                    }else if (Python.isinstance(item0, Python.importModule("numpy").attr("ndarray"))){
                        List<NDArray> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(NDArray.create(NumpyArray.INSTANCE.toJava(val.get(i))));
                        }
                        data.putListNDArray(strKey, jVal);
                    }else if (Python.isinstance(item0, Python.importModule("PIL.Image").attr("Image"))){
                        // TODO
                        throw new PythonException("Image not supprted yet.");
                    }else{ // Data
                        List<Data> jVal = new ArrayList<>();
                        for (int i=0;i<size; i++){
                            jVal.add(toJava(val.get(i)));
                        }
                        data.putListData(strKey, jVal);
                    }

                }
                else{ // Data
                    Data jVal = toJava(val);
                    data.put(strKey, jVal);
                }
            }
            return data;
        }
    }

    @Override
    public PythonObject toPython(Data javaObject) throws PythonException{
        return null;
    }

    @Override
    public boolean accepts(Object javaObject) throws PythonException{
        return javaObject instanceof Data;
    }

    @Override
    public Data adapt(Object javaObject) throws PythonException{
        if (!(javaObject instanceof Data)){
            throw new PythonException("Cannot cast " + javaObject.getClass() + " to Data.");
        }
        return (Data)javaObject;
    }





}
