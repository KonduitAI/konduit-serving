package ai.konduit.serving.models.tensorflowpython;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import org.nd4j.python4j.PythonGC;
import org.nd4j.python4j.PythonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NumpyArray {

    private PythonObject pythonObject;

    public NumpyArray(PythonObject pythonObject) {
        // TODO validate python object
        this.pythonObject = pythonObject;
    }

    public static class NumpyNDArray extends BaseNDArray<NumpyArray> {

        public NumpyNDArray(NumpyArray array) {
            super(array);
        }

        @Override
        public NDArrayType type() {
            NDArrayType type;
            String npDtype;
            try (PythonGC gc = PythonGC.watch()) {
                npDtype = ((NumpyArray) get()).getPythonObject().attr("dtype").attr("name").toString();
            }
            switch (npDtype) {
                case "float64":
                    type = NDArrayType.DOUBLE;
                    break;
                case "float32":
                    type = NDArrayType.FLOAT;
                    break;
                default:
                    try {
                        type = NDArrayType.valueOf(npDtype);
                    } catch (IllegalArgumentException iae) {
                        throw new UnsupportedOperationException("Unsupported numpy data type: " + npDtype);
                    }
            }
            return type;

        }

        @Override
        public long[] shape() {
            try (PythonGC gc = PythonGC.watch()){
                List shapeList = ((NumpyArray)get()).getPythonObject().attr("shape").toList();
                long[] shape = new long[shapeList.size()];
                for (int i =0; i<shape.length;i++){
                    shape[i] = (Long)shapeList.get(i);
                }
                return shape;
            }
        }

        @Override
        public long size(int dimension) {
            return shape()[dimension];
        }

        @Override
        public int rank() {
            return shape().length;
        }
    }

    public PythonObject getPythonObject() {
        return pythonObject;
    }

    public static class Factory implements NDArrayFactory {
        @Override
        public Set<Class<?>> supportedTypes() {
            Set<Class<?>> s = new HashSet<>();
            s.add(NumpyArray.class);
            return s;
        }

        @Override
        public boolean canCreateFrom(Object o) {
            return o instanceof NumpyArray;
        }

        @Override
        public NumpyNDArray create(Object o) {
            NumpyArray a;
            if (o instanceof NumpyArray) {
                a = (NumpyArray) o;
            } else {
                throw new IllegalStateException();
            }

            return new NumpyNDArray(a);
        }
    }
}
