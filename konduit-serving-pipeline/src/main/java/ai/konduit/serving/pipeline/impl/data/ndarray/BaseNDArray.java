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

package ai.konduit.serving.pipeline.impl.data.ndarray;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.NDArrayConverter;
import ai.konduit.serving.pipeline.api.format.NDArrayFormat;
import ai.konduit.serving.pipeline.registry.NDArrayConverterRegistry;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

@AllArgsConstructor
public abstract class BaseNDArray<T> implements NDArray {

    protected final T array;

    @Override
    public Object get() {
        return array;
    }

    @Override
    public <T> T getAs(NDArrayFormat<T> format) {
        return NDArrayConverterRegistry.getConverterFor(this, format).convert(this, format);
    }

    @Override
    public <T> T getAs(Class<T> type) {
        NDArrayConverter converter = NDArrayConverterRegistry.getConverterFor(this, type);
        Preconditions.checkState(converter != null, "No converter found for converting from %s to %s", array.getClass(), type);
        return converter.convert(this, type);
    }

    @Override
    public boolean canGetAs(NDArrayFormat<?> format) {
        NDArrayConverter converter = NDArrayConverterRegistry.getConverterFor(this, format);
        return converter != null;
    }

    @Override
    public boolean canGetAs(Class<?> type) {
        NDArrayConverter converter = NDArrayConverterRegistry.getConverterFor(this, type);
        return converter != null;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof NDArray))
            return false;

        NDArray o2 = (NDArray)o;

        //TODO is there a more efficient approach?
        SerializedNDArray thisArr = getAs(SerializedNDArray.class);
        SerializedNDArray other = o2.getAs(SerializedNDArray.class);

        return thisArr.equals(other);
    }
}
