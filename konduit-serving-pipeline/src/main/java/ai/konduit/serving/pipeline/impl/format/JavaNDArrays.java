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


}
