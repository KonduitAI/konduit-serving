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

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;

import java.util.HashSet;
import java.util.Set;

public class JavaNDArrayFactory implements NDArrayFactory {
    @Override
    public Set<Class<?>> supportedTypes() {
        Set<Class<?>> set = new HashSet<>();
        set.add(float[].class);
        set.add(float[][].class);
        set.add(float[][][].class);
        set.add(float[][][][].class);
        set.add(float[][][][][].class);
        set.add(SerializedNDArray.class);
        return set;
    }

    @Override
    public boolean canCreateFrom(Object o) {
        return supportedTypes().contains(o.getClass()); //TODO don't create set on every check
    }

    @Override
    public NDArray create(Object o) {
        if(o instanceof float[]){
            return new JavaNDArrays.Float1Array((float[]) o);
        } else if(o instanceof float[][]){
            return new JavaNDArrays.Float2Array((float[][]) o);
        } else if(o instanceof float[][][]){
            return new JavaNDArrays.Float3Array((float[][][]) o);
        } else if(o instanceof float[][][][]) {
            return new JavaNDArrays.Float4Array((float[][][][]) o);
        } else if(o instanceof float[][][][][]) {
            return new JavaNDArrays.Float5Array((float[][][][][]) o);
        } else if(o instanceof SerializedNDArray){
            return new JavaNDArrays.SNDArray((SerializedNDArray) o);
        } else {
            throw new RuntimeException("Unable to create NDArray from object: " + o.getClass());
        }
    }
}
