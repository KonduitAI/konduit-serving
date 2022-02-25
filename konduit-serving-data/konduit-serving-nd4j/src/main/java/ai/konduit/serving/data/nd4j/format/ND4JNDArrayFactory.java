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

import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.format.NDArrayFactory;
import ai.konduit.serving.pipeline.impl.data.wrappers.ListValue;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

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
        return o instanceof INDArray || o instanceof ListValue;
    }

    @Override
    public NDArray create(Object o) {
        Preconditions.checkState(canCreateFrom(o), "Unable to create ND4J NDArray from object of %s", o.getClass());

        INDArray a;
        if(o instanceof INDArray){
            a = (INDArray)o;
        } else if (o instanceof ListValue) {
            List<Long> shape = new ArrayList<>();
            List<Float> data = new ArrayList<>();

            ListValue listValue = (ListValue) o;
            getData(listValue, data);
            getShape(listValue, shape);
            a = Nd4j.create(Floats.toArray(data), Longs.toArray(shape));
        } else {
            throw new IllegalStateException();
        }

        //TODO add all the other java types!

        return new ND4JNDArray(a);
    }

    private void getData(ListValue listValue, List<Float> data) {
        for (Object object: listValue.get()) {
            if(listValue.elementType() == ValueType.LIST) {
                if(object instanceof Pair) {
                    Pair pair = (Pair) object;
                    getData(new ListValue((List) pair.getKey(), ValueType.LIST), data);
                } else {
                    if(object instanceof Double || object instanceof Long) {
                        data.add(Float.valueOf(String.valueOf(object)));
                    } else {
                        throw new IllegalStateException(String.format("Can't convert type %s to an NDArray", object.getClass().getCanonicalName()));
                    }
                }
            } else if(listValue.elementType() == ValueType.DOUBLE || listValue.elementType() == ValueType.INT64) {
                data.add(Float.valueOf(String.valueOf(object)));
            } else {
                throw new IllegalStateException(String.format("Can't convert type %s to an NDArray", listValue.elementType()));
            }
        }
    }

    private void getShape(ListValue listValue, List<Long> shape) {
        if(listValue.get() == null || listValue.get().isEmpty()) {
            throw new IllegalStateException("Empty or zero sized arrays are not accepted!");
        } else {
            shape.add((long) listValue.get().size());

            if (listValue.elementType() == ValueType.LIST) {
                if(listValue.get().get(0) instanceof Pair) {
                    Pair pair = (Pair) listValue.get().get(0);
                    ListValue listValueInternal = new ListValue((List) pair.getKey(), ValueType.LIST);
                    getShape(listValueInternal, shape);
                }
            }
        }
    }
}
