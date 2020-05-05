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

import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.HashSet;
import java.util.Set;

public class ND4JNDArrayFactory implements NDArrayFactory {
    @Override
    public Set<Class<?>> supportedTypes() {
        Set<Class<?>> s = new HashSet<>();
        s.add(INDArray.class);

        //TODO do we want to allow creating INDArray from float[], float[][] etc?
        // Probably not, given we can convert behind the scenes easily if needed...

        return s;
    }

    @Override
    public boolean canCreateFrom(Object o) {
        return o instanceof INDArray;
    }

    @Override
    public NDArray create(Object o) {
        Preconditions.checkState(canCreateFrom(o), "Unable to create ND4J NDArray from object of %s", o.getClass());

        INDArray a;
        if(o instanceof INDArray){
            a = (INDArray)o;
        } else {
            throw new IllegalStateException();
        }

        //TODO add all the other java types!

        return new ND4JNDArray(a);
    }
}
