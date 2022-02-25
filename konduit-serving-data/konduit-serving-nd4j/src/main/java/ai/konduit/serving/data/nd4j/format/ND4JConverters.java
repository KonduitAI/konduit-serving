/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.data.nd4j.format;

import ai.konduit.serving.data.nd4j.util.ND4JUtil;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.util.ArrayUtil;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.nio.ByteBuffer;

public class ND4JConverters  {


    @AllArgsConstructor
    public static abstract class BaseFromNd4jArrConverter<T> implements NDArrayConverter {
        private final Class<T> cTo;

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            boolean ret = INDArray.class.isAssignableFrom(from.get().getClass()) && to.isAssignableFrom(cTo);
            return ret;
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException("Not yet implemneted");
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            INDArray arr = (INDArray) from.get();
            return (U)convert(arr);
        }

        public abstract T convert(INDArray from);
    }

    public static class ArrToFloat1Converter extends BaseFromNd4jArrConverter<float[]> {
        public ArrToFloat1Converter() { super(float[].class); }
        @Override
        public float[] convert(INDArray from) {
            return from.toFloatVector();
        }
    }

    public static class ArrToFloat2Converter extends BaseFromNd4jArrConverter<float[][]> {
        public ArrToFloat2Converter() { super(float[][].class); }
        @Override
        public float[][] convert(INDArray from) {
            return from.toFloatMatrix();
        }
    }

    public static class ArrToFloat3Converter extends BaseFromNd4jArrConverter<float[][][]> {
        public ArrToFloat3Converter() { super(float[][][].class); }
        @Override
        public float[][][] convert(INDArray from) {
            Preconditions.checkState(from.rank() == 3, "Can only convert rank 3 arrays to float[][][], got array with shape %s", from.shape());
            float[][][] out = new float[(int)from.size(0)][0][0];
            for( int i = 0; i < out.length; i++) {
                out[i] = from.get(NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.all()).toFloatMatrix();
            }
            return out;
        }
    }

    public static class ArrToFloat4Converter extends BaseFromNd4jArrConverter<float[][][][]> {
        public ArrToFloat4Converter() { super(float[][][][].class); }
        @Override
        public float[][][][] convert(INDArray from) {
            Preconditions.checkState(from.rank() == 4, "Can only convert rank 4 arrays to float[][][][], got array with shape %s", from.shape());
            float[][][][] out = new float[(int)from.size(0)][(int)from.size(1)][0][0];
            for( int i=0; i<out.length; i++){
                for( int j = 0; j < out[0].length; j++) {
                    out[i][j] = from.get(NDArrayIndex.point(i), NDArrayIndex.point(j), NDArrayIndex.all(), NDArrayIndex.all()).toFloatMatrix();
                }
            }
            return out;
        }
    }

    public static class ArrToFloat5Converter extends BaseFromNd4jArrConverter<float[][][][][]> {
        public ArrToFloat5Converter() { super(float[][][][][].class); }
        @Override
        public float[][][][][] convert(INDArray from) {
            Preconditions.checkState(from.rank() == 5, "Can only convert rank 5 arrays to float[][][][][], got array with shape %s", from.shape());
            float[][][][][] out = new float[(int)from.size(0)][(int)from.size(1)][0][0][0];
            for( int i = 0; i < out.length; i++) {
                for( int j = 0; j < out[0].length; j++) {
                    for(int k = 0; k < out[2].length; k++)
                    out[i][j][k] = from.get(NDArrayIndex.point(i), NDArrayIndex.point(j), NDArrayIndex.point(k), NDArrayIndex.all()).toFloatMatrix();
                }
            }
            return out;
        }
    }




    @AllArgsConstructor
    public static abstract class BaseToNd4jArrConverter<T> implements NDArrayConverter {
        private final Class<T> clazz;
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return clazz.isAssignableFrom(from.get().getClass());       //Basically: return from.get() instanceof clazz
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return clazz.isAssignableFrom(from.get().getClass()) && INDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert NDArray to %s", to);
            T t = (T) from.get();
            INDArray out = convert(t);
            return (U)out;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            T t = (T) from.get();
            INDArray arr = convert(t);
            return (U)arr;
        }

        public abstract INDArray convert(T from);
    }

    public static class Float1ToArrConverter extends BaseToNd4jArrConverter<float[]> {
        public Float1ToArrConverter() { super(float[].class); }

        @Override
        public INDArray convert(float[] from) {
            return Nd4j.createFromArray(from);
        }
    }

    public static class Float2ToArrConverter extends BaseToNd4jArrConverter<float[][]> {
        public Float2ToArrConverter() { super(float[][].class); }

        @Override
        public INDArray convert(float[][] from) {
            return Nd4j.createFromArray(from);
        }
    }

    public static class Float3ToArrConverter extends BaseToNd4jArrConverter<float[][][]> {
        public Float3ToArrConverter() { super(float[][][].class); }

        @Override
        public INDArray convert(float[][][] from) {
            return Nd4j.createFromArray(from);
        }
    }

    public static class Float4ToArrConverter extends BaseToNd4jArrConverter<float[][][][]> {
        public Float4ToArrConverter() { super(float[][][][].class); }

        @Override
        public INDArray convert(float[][][][] from) {
            return Nd4j.createFromArray(from);
        }
    }

    public static class Float5ToArrConverter extends BaseToNd4jArrConverter<float[][][][][]> {
        public Float5ToArrConverter() { super(float[][][][][].class); }

        @Override
        public INDArray convert(float[][][][][] array) {
            Preconditions.checkNotNull(array, "Cannot create INDArray from null Java array");
            ArrayUtil.assertNotRagged(array);
            if(array.length == 0 || array[0].length == 0 || array[0][0].length == 0 || array[0][0][0].length == 0)
                return Nd4j.empty(DataType.FLOAT);
            long[] shape = new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length};

            return Nd4j.create(flatten(array), shape, ArrayUtil.calcStrides(shape), 'c', DataType.FLOAT);
        }
    }

    public static float[] flatten(float[][][][][] arr) {
        float[] ret = new float[arr.length * arr[0].length * arr[0][0].length * arr[0][0][0].length * arr[0][0][0][0].length];
        int count = 0;

        for(int i = 0; i < arr.length; ++i) {
            for(int j = 0; j < arr[0].length; ++j) {
                for(int k = 0; k < arr[0][0].length; ++k) {
                    System.arraycopy(arr[i][j][k], 0, ret, count, arr[0][0][0].length);
                    count += arr[0][0][0].length;
                }
            }
        }

        return ret;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @AllArgsConstructor
    public static class SerializedToNd4jArrConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return SerializedNDArray.class.isAssignableFrom(from.get().getClass()) && INDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert NDArray to %s", to);
            SerializedNDArray t = (SerializedNDArray) from.get();
            INDArray out = convert(t);
            return (U)out;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            SerializedNDArray f = (SerializedNDArray) from.get();
            INDArray arr = convert(f);
            return (U)arr;
        }

        public INDArray convert(SerializedNDArray from){
            DataType dt = ND4JUtil.typeNDArrayTypeToNd4j(from.getType());
            long[] shape = from.getShape();
            long length = ArrayUtil.prodLong(shape);

            ByteBuffer bb = from.getBuffer();
            bb.rewind();

            DataBuffer db = Nd4j.createBuffer(bb, dt, (int)length, 0);
            INDArray arr = Nd4j.create(db, shape);
            return arr;
        }
    }


    @AllArgsConstructor
    public static class Nd4jToSerializedConverter implements NDArrayConverter {
        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return INDArray.class.isAssignableFrom(from.get().getClass()) && SerializedNDArray.class.isAssignableFrom(to);
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert SerializedNDArray to %s", to);
            INDArray f = (INDArray) from.get();
            SerializedNDArray t = convert(f);
            return (U)t;
        }

        @Override
        public <U> U convert(NDArray from, NDArrayFormat<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            INDArray f = (INDArray) from.get();
            SerializedNDArray t = convert(f);
            return (U)t;
        }

        public SerializedNDArray convert(INDArray from){
            if(from.isView() || from.ordering() != 'c' || !Shape.hasDefaultStridesForShape(from))
                from = from.dup('c');

            NDArrayType type = ND4JUtil.typeNd4jToNDArrayType(from.dataType());
            long[] shape = from.shape();
            ByteBuffer bb = from.data().asNio();

            return new SerializedNDArray(type, shape, bb);
        }
    }
}
