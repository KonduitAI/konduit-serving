package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.data.JData;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.eclipse.python4j.*;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PyData extends PythonType<Data> {

    public static final PyData INSTANCE = new PyData();
    private static Boolean isPillowInstalled = null;

    public PyData() {
        super("Data", Data.class);
    }

    private static boolean isPillowInstalled() {
        if (isPillowInstalled == null) {
            try {
                Python.importModule("PIL.Image");
                isPillowInstalled = true;
            } catch (PythonException pe) {
                isPillowInstalled = false;
            }
        }
        return isPillowInstalled;
    }

    @Override
    public Data toJava(PythonObject pythonObject) throws PythonException {
        JData data = new JData();
        try (PythonGC gc = PythonGC.watch()) {
            PythonObject pyKeysList = Python.list(pythonObject.attr("keys").call());
            List keysList = PythonTypes.LIST.toJava(pyKeysList);
            for (Object key : keysList) {
                if (!(key instanceof String)) {
                    throw new PythonException("String key expected."); // already checked in python
                }
                String strKey = (String) key;
                PythonObject val = pythonObject.get(strKey);
                if (Python.isinstance(val, Python.strType())) {
                    String jVal = PythonTypes.STR.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.intType())) {
                    long jVal = PythonTypes.INT.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.floatType())) {
                    double jVal = PythonTypes.FLOAT.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.boolType())) {
                    boolean jVal = PythonTypes.BOOL.toJava(val);
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.memoryviewType())) {
                    BytePointer bp = PythonTypes.MEMORYVIEW.toJava(val);
                    byte[] jVal = bp.getStringBytes();
                    data.put(strKey, jVal);
                } else if ( Python.isinstance(val, Python.importModule("numpy").attr("ndarray"))) {
                    INDArray arr = NumpyArray.INSTANCE.toJava(val);
                    NDArray jVal = NDArray.create(arr);
                    data.put(strKey, jVal);
                } else if (isPillowInstalled() && Python.isinstance(val, Python.importModule("PIL.Image").attr("Image"))) {
                    // TODO
                    throw new PythonException("Image not supprted yet.");
                } else if (Python.type(val).attr("__name__").toString().equals("BoundingBox")) {
                    BoundingBox jVal = BoundingBox.create(val.attr("cx").toDouble(),
                            val.attr("cy").toDouble(),
                            val.attr("height").toDouble(),
                            val.attr("width").toDouble(),
                            val.attr("label").toString(),
                            val.attr("probability").toDouble());
                    data.put(strKey, jVal);
                } else if (Python.isinstance(val, Python.listType())) {
                    long size = Python.len(val).toLong();
                    if (size == 0) {
                        throw new PythonException("Cannot infer type from empty list.");
                    }
                    PythonObject item0 = val.get(0);
                    if (Python.isinstance(item0, Python.strType())) {
                        List<String> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(PythonTypes.STR.toJava(val.get(i)));
                        }
                        data.putListString(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.intType())) {
                        List<Long> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(PythonTypes.INT.toJava(val.get(i)));
                        }
                        data.putListInt64(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.floatType())) {
                        List<Double> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(PythonTypes.FLOAT.toJava(val.get(i)));
                        }
                        data.putListDouble(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.boolType())) {
                        List<Boolean> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(PythonTypes.BOOL.toJava(val.get(i)));
                        }
                        data.putListBoolean(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.memoryviewType())) {
                        List<byte[]> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(PythonTypes.MEMORYVIEW.toJava(val.get(i)).getStringBytes());
                        }
                        data.putListBytes(strKey, jVal);
                    } else if (Python.isinstance(item0, Python.importModule("numpy").attr("ndarray"))) {
                        List<NDArray> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            jVal.add(NDArray.create(NumpyArray.INSTANCE.toJava(val.get(i))));
                        }
                        data.putListNDArray(strKey, jVal);
                    } else if (isPillowInstalled() && Python.isinstance(item0, Python.importModule("PIL.Image").attr("Image"))) {
                        // TODO
                        throw new PythonException("Image not supprted yet.");
                    } else if (Python.type(item0).attr("__name__").toString().equals("BoundingBox")) {
                        List<BoundingBox> jVal = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            BoundingBox bbox = BoundingBox.create(val.attr("cx").toDouble(),
                                    val.attr("cy").toDouble(),
                                    val.attr("height").toDouble(),
                                    val.attr("width").toDouble(),
                                    val.attr("label").toString(),
                                    val.attr("probability").toDouble());
                            jVal.add(bbox);
                        }
                        data.putListBoundingBox(strKey, jVal);
                    } else { // Data

//                        List<Data> jVal = new ArrayList<>();
//                        for (int i = 0; i < size; i++) {
//                            jVal.add(toJava(val.get(i)));
//                        }
//                        data.putListData(strKey, jVal);
                        throw new PythonException("Unsupported type in list: " + Python.type(item0));
                    }

                } else { // Data
                    Data jVal = toJava(val);
                    data.put(strKey, jVal);
                }
            }
            return data;
        }
    }


    @Override
    public PythonObject toPython(Data javaObject) throws PythonException {
        PythonObject dataCls = Python.globals().attr("get").call("Data");
        if (dataCls.isNone()) {
            String baseCode;
            try (InputStream is = PyData.class
                    .getResourceAsStream("pydata.py")) {
                baseCode = IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new PythonException("Error reading pydata.py", e);
            }
            PythonExecutioner.exec(baseCode);
            dataCls = Python.globals().get("Data");
        }
        if (dataCls.isNone()) {
            throw new PythonException("Unable to get Data class.");
        }


        try (PythonGC gc = PythonGC.watch()) {
            PythonObject data = dataCls.call();
            for (String key : javaObject.keys()) {
                PythonObject pyKey = new PythonObject(key);
                switch (javaObject.type(key)) {
                    case STRING:
                    case INT64:
                    case DOUBLE:
                    case BOOLEAN:
                        data.set(pyKey, PythonTypes.convert(javaObject.get(key)));
                        break;
                    case BYTES:
                        data.set(pyKey, PythonTypes.MEMORYVIEW.toPython(new BytePointer(javaObject.getBytes(key))));
                        break;
                    case NDARRAY:
                        data.set(pyKey, NumpyArray.INSTANCE.toPython(javaObject.getNDArray(key).getAs(INDArray.class)));
                        break;
                    case BOUNDING_BOX:
                        PythonObject bboxCls = Python.globals().get("BoundingBox");
                        BoundingBox jBbox = javaObject.getBoundingBox(key);
                        PythonObject bbox = bboxCls.call(jBbox.cx(), jBbox.cy(), jBbox.height(), jBbox.width(), jBbox.label(), jBbox.probability());
                        data.set(pyKey, bbox);
                        break;
                    case IMAGE:
                        // TODO
                        throw new PythonException("Image not supprted yet.");
                    case LIST:
                        List<PythonObject> list = new ArrayList<>();
                        switch (javaObject.listType(key)) {
                            case STRING:
                            case INT64:
                            case DOUBLE:
                            case BOOLEAN:
                                for (Object item : javaObject.getList(key, javaObject.listType(key))) {
                                    list.add(PythonTypes.convert(item));
                                }
                                break;
                            case BYTES:
                                for (byte[] item : javaObject.getListBytes(key)) {
                                    list.add(PythonTypes.MEMORYVIEW.toPython(new BytePointer(item)));
                                }
                                break;
                            case NDARRAY:
                                for (NDArray item : javaObject.getListNDArray(key)) {
                                    list.add(NumpyArray.INSTANCE.toPython(item.getAs(INDArray.class)));
                                }
                                break;
                            case BOUNDING_BOX:
                                for (BoundingBox item : javaObject.getListBoundingBox(key)) {
                                    list.add(
                                            Python.globals().get("BoundingBox").call(
                                                    item.cx(),
                                                    item.cy(),
                                                    item.height(),
                                                    item.width(),
                                                    item.label(),
                                                    item.probability()
                                            )
                                    );
                                }
                                break;
                            case IMAGE:
                                // TODO
                                throw new PythonException("Image not supprted yet.");
                            case DATA:
                            default:
                                throw new PythonException("Unsupported type in list: " + javaObject.listType(key));
                        }
                        data.set(pyKey, PythonTypes.LIST.toPython(list));
                    case DATA:
                        data.set(pyKey, toPython(javaObject.getData(key)));
                        break;
                    default:
                        throw new PythonException("Unsupported type: " + javaObject.type(key));
                }
            }
            PythonGC.keep(data);
            return data;
        }
    }

    @Override
    public boolean accepts(Object javaObject) throws PythonException {
        return javaObject instanceof Data;
    }

    @Override
    public Data adapt(Object javaObject) throws PythonException {
        if (!(javaObject instanceof Data)) {
            throw new PythonException("Cannot cast " + javaObject.getClass() + " to Data.");
        }
        return (Data) javaObject;
    }


}
