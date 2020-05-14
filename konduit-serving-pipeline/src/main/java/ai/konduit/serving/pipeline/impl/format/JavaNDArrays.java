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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.BaseNDArray;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;

public class JavaNDArrays {

    public static class SNDArray extends BaseNDArray<SerializedNDArray> {
        public SNDArray(SerializedNDArray array) {
            super(array);
        }

        @Override
        public NDArrayType type() {
            return array.getType();
        }

        @Override
        public long[] shape() {
            return array.getShape();
        }
    }

    private static abstract class BaseFloatArray<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseFloatArray(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.FLOAT;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Float1Array extends BaseFloatArray<float[]>{
        public Float1Array(float[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Float2Array extends BaseFloatArray<float[][]>{
        public Float2Array(float[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Float3Array extends BaseFloatArray<float[][][]>{
        public Float3Array(float[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Float4Array extends BaseFloatArray<float[][][][]>{
        public Float4Array(float[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Float5Array extends BaseFloatArray<float[][][][][]>{
        public Float5Array(float[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});
        }
    }

    private static abstract class BaseDoubleArray<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseDoubleArray(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.DOUBLE;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Double1Array extends BaseDoubleArray<double[]>{
        public Double1Array(double[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Double2Array extends BaseDoubleArray<double[][]>{
        public Double2Array(double[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Double3Array extends BaseDoubleArray<double[][][]>{
        public Double3Array(double[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Double4Array extends BaseDoubleArray<double[][][][]>{
        public Double4Array(double[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Double5Array extends BaseDoubleArray<double[][][][][]>{
        public Double5Array(double[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseBfloat16Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseBfloat16Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.BFLOAT16;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Bfloat161Array extends BaseBfloat16Array<float[]>{
        public Bfloat161Array(float[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Bfloat162Array extends BaseBfloat16Array<float[][]>{
        public Bfloat162Array(float[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Bfloat163Array extends BaseBfloat16Array<float[][][]>{
        public Bfloat163Array(float[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Bfloat164Array extends BaseBfloat16Array<float[][][][]>{
        public Bfloat164Array(float[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Bfloat165Array extends BaseBfloat16Array<float[][][][][]>{
        public Bfloat165Array(float[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseFloat16Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseFloat16Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.FLOAT16;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Float161Array extends BaseFloat16Array<float[]>{
        public Float161Array(float[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Float162Array extends BaseFloat16Array<float[][]>{
        public Float162Array(float[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Float163Array extends BaseFloat16Array<float[][][]>{
        public Float163Array(float[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Float164Array extends BaseFloat16Array<float[][][][]>{
        public Float164Array(float[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Float165Array extends BaseFloat16Array<float[][][][][]>{
        public Float165Array(float[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseBoolArray<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseBoolArray(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.BOOL;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Bool1Array extends BaseBoolArray<boolean[]>{
        public Bool1Array(boolean[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Bool2Array extends BaseBoolArray<boolean[][]>{
        public Bool2Array(boolean[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Bool3Array extends BaseBoolArray<boolean[][][]>{
        public Bool3Array(boolean[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Bool4Array extends BaseBoolArray<boolean[][][][]>{
        public Bool4Array(boolean[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Bool5Array extends BaseBoolArray<boolean[][][][][]>{
        public Bool5Array(boolean[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseInt8Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseInt8Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.INT8;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Int81Array extends BaseInt8Array<byte[]>{
        public Int81Array(byte[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Int82Array extends BaseInt8Array<byte[][]>{
        public Int82Array(byte[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Int83Array extends BaseInt8Array<byte[][][]>{
        public Int83Array(byte[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Int84Array extends BaseInt8Array<byte[][][][]>{
        public Int84Array(byte[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Int85Array extends BaseInt8Array<byte[][][][][]>{
        public Int85Array(byte[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseInt16Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseInt16Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.INT16;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Int161Array extends BaseInt16Array<short[]>{
        public Int161Array(short[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Int162Array extends BaseInt16Array<short[][]>{
        public Int162Array(short[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Int163Array extends BaseInt16Array<short[][][]>{
        public Int163Array(short[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Int164Array extends BaseInt16Array<short[][][][]>{
        public Int164Array(short[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Int165Array extends BaseInt16Array<short[][][][][]>{
        public Int165Array(short[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseInt32Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseInt32Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.INT32;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Int321Array extends BaseInt32Array<int[]>{
        public Int321Array(int[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Int322Array extends BaseInt32Array<int[][]>{
        public Int322Array(int[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Int323Array extends BaseInt32Array<int[][][]>{
        public Int323Array(int[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Int324Array extends BaseInt32Array<int[][][][]>{
        public Int324Array(int[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Int325Array extends BaseInt32Array<int[][][][][]>{
        public Int325Array(int[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseInt64Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseInt64Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.INT64;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Int641Array extends BaseInt64Array<long[]>{
        public Int641Array(long[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Int642Array extends BaseInt64Array<long[][]>{
        public Int642Array(long[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Int643Array extends BaseInt64Array<long[][][]>{
        public Int643Array(long[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Int644Array extends BaseInt64Array<long[][][][]>{
        public Int644Array(long[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Int645Array extends BaseInt64Array<long[][][][][]>{
        public Int645Array(long[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseUint8Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseUint8Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.UINT8;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Uint81Array extends BaseUint8Array<byte[]>{
        public Uint81Array(byte[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Uint82Array extends BaseUint8Array<byte[][]>{
        public Uint82Array(byte[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Uint83Array extends BaseUint8Array<byte[][][]>{
        public Uint83Array(byte[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Uint84Array extends BaseUint8Array<byte[][][][]>{
        public Uint84Array(byte[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Uint85Array extends BaseUint8Array<byte[][][][][]>{
        public Uint85Array(byte[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseUint16Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseUint16Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.UINT16;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Uint161Array extends BaseUint16Array<short[]>{
        public Uint161Array(short[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Uint162Array extends BaseUint16Array<short[][]>{
        public Uint162Array(short[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Uint163Array extends BaseUint16Array<short[][][]>{
        public Uint163Array(short[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Uint164Array extends BaseUint16Array<short[][][][]>{
        public Uint164Array(short[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Uint165Array extends BaseUint8Array<short[][][][][]>{
        public Uint165Array(short[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseUint32Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseUint32Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.UINT32;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Uint321Array extends BaseUint32Array<int[]>{
        public Uint321Array(int[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Uint322Array extends BaseUint32Array<int[][]>{
        public Uint322Array(int[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Uint323Array extends BaseUint32Array<int[][][]>{
        public Uint323Array(int[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Uint324Array extends BaseUint32Array<int[][][][]>{
        public Uint324Array(int[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Uint325Array extends BaseUint32Array<int[][][][][]>{
        public Uint325Array(int[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }

    private static abstract class BaseUint64Array<T> extends BaseNDArray<T>{
        protected final long[] shape;
        public BaseUint64Array(T array, long[] shape) {
            super(array);
            this.shape = shape;
        }

        @Override
        public NDArrayType type() {
            return NDArrayType.UINT64;
        }

        @Override
        public long[] shape() {
            return shape;
        }
    }

    public static class Uint641Array extends BaseUint64Array<long[]>{
        public Uint641Array(long[] array) {
            super(array, new long[]{array.length});
        }
    }

    public static class Uint642Array extends BaseUint8Array<long[][]>{
        public Uint642Array(long[][] array) {
            super(array, new long[]{array.length, array[0].length});
        }
    }

    public static class Uint643Array extends BaseUint64Array<long[][][]>{
        public Uint643Array(long[][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length});
        }
    }

    public static class Uint644Array extends BaseUint64Array<long[][][][]>{
        public Uint644Array(long[][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length});
        }
    }

    public static class Uint645Array extends BaseUint64Array<long[][][][][]>{
        public Uint645Array(long[][][][][] array) {
            super(array, new long[]{array.length, array[0].length, array[0][0].length, array[0][0][0].length, array[0][0][0][0].length});

        }
    }
}
