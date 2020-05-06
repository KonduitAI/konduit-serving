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

package ai.konduit.serving.pipeline.impl.data.java;

import ai.konduit.serving.pipeline.api.data.BaseNDArray;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;

public class JavaNDArrays {

    public static class Float1Array extends BaseNDArray<float[]>{
        public Float1Array(float[] array) {
            super(array);
        }
    }

    public static class Float2Array extends BaseNDArray<float[][]>{
        public Float2Array(float[][] array) {
            super(array);
        }
    }

    public static class Float3Array extends BaseNDArray<float[][][]>{
        public Float3Array(float[][][] array) {
            super(array);
        }
    }

    public static class Float4Array extends BaseNDArray<float[][][][]>{
        public Float4Array(float[][][][] array) {
            super(array);
        }
    }


}