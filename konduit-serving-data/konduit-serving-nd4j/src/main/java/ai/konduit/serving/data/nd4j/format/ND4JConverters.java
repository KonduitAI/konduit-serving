/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
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

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

public class ND4JConverters  {


    ///Normally we'd just do a lamda here - but we also need to be able to refer to the class name for the service loader

    public static final NDArrayConverter ArrToFloat1 = new Float1ToArrConverter();

    public static final NDArrayConverter ArrToFloat2 = new Float2ToArrConverter();

    public static final NDArrayConverter ArrToFloat3 = new Float3ToArrConverter();

    public static final NDArrayConverter ArrToFloat4 = new Float4ToArrConverter();





    //////////////////////////////////////////////////////////////////////////////////////////

    @AllArgsConstructor
    public static abstract class BaseFromNd4jArrConverter<T> implements NDArrayConverter {
        private final Class<T> clazz;

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat to) {
            return canConvert(from, to.formatType());
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return clazz == to;
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

    protected static class ArrToFloat1Converter extends BaseFromNd4jArrConverter<float[]> {
        public ArrToFloat1Converter() { super(float[].class); }
        @Override
        public float[] convert(INDArray from) {
            return from.toFloatVector();
        }
    }

    protected static class ArrToFloat2Converter extends BaseFromNd4jArrConverter<float[][]> {
        public ArrToFloat2Converter() { super(float[][].class); }
        @Override
        public float[][] convert(INDArray from) {
            return from.toFloatMatrix();
        }
    }

    protected static class ArrToFloat3Converter extends BaseFromNd4jArrConverter<float[][][]> {
        public ArrToFloat3Converter() { super(float[][][].class); }
        @Override
        public float[][][] convert(INDArray from) {
            Preconditions.checkState(from.rank() == 3, "Can only convert rank 3 arrays to float[][][], got array with shape %s", from.shape());
            float[][][] out = new float[(int)from.size(0)][0][0];
            for( int i=0; i<out.length; i++){
                out[i] = from.get(NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.all()).toFloatMatrix();
            }
            return out;
        }
    }

    protected static class ArrToFloat4Converter extends BaseFromNd4jArrConverter<float[][][][]> {
        public ArrToFloat4Converter() { super(float[][][][].class); }
        @Override
        public float[][][][] convert(INDArray from) {
            Preconditions.checkState(from.rank() == 4, "Can only convert rank 4 arrays to float[][][][], got array with shape %s", from.shape());
            float[][][][] out = new float[(int)from.size(0)][(int)from.size(1)][0][0];
            for( int i=0; i<out.length; i++){
                for( int j=0; j<out[0].length; j++){
                    out[i][j] = from.get(NDArrayIndex.point(i), NDArrayIndex.point(j), NDArrayIndex.all(), NDArrayIndex.all()).toFloatMatrix();
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
            return clazz.isAssignableFrom(from.get().getClass());
        }

        @Override
        public <U> U convert(NDArray from, Class<U> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert NDArray to %s", to);
            U t = (U) from.get();
            return (U)NDArray.create(convert((T)t));
        }

        @Override
        public NDArray convert(NDArray from, NDArrayFormat to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert to format: %s", to);
            T t = (T) from.get();
            INDArray arr = convert(t);
            return NDArray.create(arr);
        }

        public abstract INDArray convert(T from);
    }

    protected static class Float1ToArrConverter extends BaseToNd4jArrConverter<float[]> {
        public Float1ToArrConverter() { super(float[].class); }

        @Override
        public INDArray convert(float[] from) {
            return Nd4j.createFromArray(from);
        }
    }

    protected static class Float2ToArrConverter extends BaseToNd4jArrConverter<float[][]> {
        public Float2ToArrConverter() { super(float[][].class); }

        @Override
        public INDArray convert(float[][] from) {
            return Nd4j.createFromArray(from);
        }
    }

    protected static class Float3ToArrConverter extends BaseToNd4jArrConverter<float[][][]> {
        public Float3ToArrConverter() { super(float[][][].class); }

        @Override
        public INDArray convert(float[][][] from) {
            return Nd4j.createFromArray(from);
        }
    }

    protected static class Float4ToArrConverter extends BaseToNd4jArrConverter<float[][][][]> {
        public Float4ToArrConverter() { super(float[][][][].class); }

        @Override
        public INDArray convert(float[][][][] from) {
            return Nd4j.createFromArray(from);
        }
    }



}
