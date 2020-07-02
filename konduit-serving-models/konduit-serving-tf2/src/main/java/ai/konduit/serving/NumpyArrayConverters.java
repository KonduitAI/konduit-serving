package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.AllArgsConstructor;
import org.nd4j.python4j.*;

import java.nio.ByteBuffer;
import java.util.List;

public class NumpyArrayConverters {

    @AllArgsConstructor
    public static class SerializedToNumpyArrayConverter implements NDArrayConverter {

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return SerializedNDArray.class.isAssignableFrom(from.get().getClass()) && NumpyArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            SerializedNDArray t = (SerializedNDArray) from.get();
            NumpyArray out = convert(t);
            return (U) out;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            SerializedNDArray f = (SerializedNDArray) from.get();
            NumpyArray arr = convert(f);
            return (U) arr;
        }

        private NumpyArray convert(SerializedNDArray from) {
            NDArrayType type = from.getType();
            if (!type.isFixedWidth()) {
                throw new UnsupportedOperationException("Variable width data types are not supported yet.");
            }
            String npDType;
            switch (type) {
                case BFLOAT16:
                    // TODO
                    throw new UnsupportedOperationException("BFloat16 is not supported yet.");
                case FLOAT:
                    npDType = "float32";
                    break;
                default:
                    npDType = type.name().toLowerCase();

            }

            ByteBuffer bb = from.getBuffer();
            byte[] bytes = bb.array();

            try (PythonGC gc = PythonGC.watch()) {
                PythonObject fromBuffer = Python.importModule("numpy").attr("frombuffer");
                PythonObject npArr = fromBuffer.call(bytes, npDType);
                npArr = npArr.attr("reshape").call(from.getShape());
                PythonGC.keep(npArr);
                return new NumpyArray(npArr);
            }

        }
    }

    public static class NumpyArrayToSerializedConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return NumpyArray.class.isAssignableFrom(from.get().getClass()) && SerializedNDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            NumpyArray f = (NumpyArray) from.get();
            SerializedNDArray t = convert(f);
            return (U) t;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            if (!canConvert(from, to)) {
                throw new IllegalArgumentException("Unable to convert NDArray to " + to);
            }
            NumpyArray f = (NumpyArray) from.get();
            SerializedNDArray t = convert(f);
            return (U) t;
        }

        public SerializedNDArray convert(NumpyArray from) {
            try (PythonGC gc = PythonGC.watch()) {
                NDArrayType type;
                String npDtype = from.getPythonObject().attr("dtype").attr("name").toString();
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
                List shapeList = PythonTypes.LIST.toJava(from.getPythonObject().attr("shape"));
                long[] shape = new long[shapeList.size()];
                for (int i = 0; i < shape.length; i++) {
                    shape[i] = (Long)shapeList.get(i);
                }
                byte[] bytes = PythonTypes.BYTES.toJava(Python.bytes(from.getPythonObject()));
                return new SerializedNDArray(type, shape, ByteBuffer.wrap(bytes));
            }

        }
    }
}
