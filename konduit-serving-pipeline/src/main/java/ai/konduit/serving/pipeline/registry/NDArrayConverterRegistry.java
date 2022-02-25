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

package ai.konduit.serving.pipeline.registry;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.*;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.nd4j.common.primitives.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NDArrayConverterRegistry extends AbstractRegistry<NDArrayConverter> {

    private static final NDArrayConverterRegistry INSTANCE = new NDArrayConverterRegistry();

    protected NDArrayConverterRegistry(){
        super(NDArrayConverter.class);
    }

    public static int numFactories(){
        return INSTANCE.registryNumFactories();
    }

    public static List<NDArrayConverter> getFactories(){
        return INSTANCE.registryGetFactories();
    }

    public static NDArrayConverter getFactoryFor(@NonNull Object o){
        return INSTANCE.registryGetFactoryFor(o);
    }

    @Override
    public boolean acceptFactory(NDArrayConverter factory, Object o) {
        Pair<NDArray, NDArrayFormat> p = (Pair<NDArray, NDArrayFormat>) o;
        return factory.canConvert(p.getFirst(), p.getSecond());
    }

    @Override
    public Set<Class<?>> supportedForFactory(NDArrayConverter factory) {
        return Collections.emptySet();
    }

    public static NDArrayConverter getConverterFor(NDArray arr, Class<?> type ){
        return INSTANCE.getConverterForClass(arr, type);
    }

    public static NDArrayConverter getConverterFor(NDArray arr, NDArrayFormat<?> type ){
        return INSTANCE.getConverterForType(arr, type);
    }

    public NDArrayConverter getConverterForClass(NDArray arr, Class<?> type ){
        if(factories == null)
            init();

        if(factoriesMap.containsKey(type)){
            return factoriesMap.get(type).get(0);       //TODO multiple converters
        }

        for(NDArrayConverter c : factories){
            if(c.canConvert(arr, type)){
                return c;
            }
        }

        //No factory is available. Try to fall back on X -> SerializedNDArray -> Y
        if(type != SerializedNDArray.class && !(arr.get() instanceof SerializedNDArray)){
            NDArrayConverter c1 = getConverterForClass(arr, SerializedNDArray.class);
            if(c1 != null){
                NDArray arr2 = NDArray.create(c1.convert(arr, SerializedNDArray.class));            //TODO this is ugly - we throw this result away!
                NDArrayConverter c2 = getConverterForClass(arr2, type);
                return new TwoStepNDArrayConverter(arr.get().getClass(), type, c1, c2);
            }
        }

        return null;
    }

    public NDArrayConverter getConverterForType(NDArray arr, NDArrayFormat<?> type ){
        if(factories == null)
            init();

        for(NDArrayConverter c : factories){
            if(c.canConvert(arr, type)){
                return c;
            }
        }
        return null;
    }

    public static void addConverter(NDArrayConverter f){
        INSTANCE.addFactoryInstance(f);
    }

    @AllArgsConstructor
    private static class TwoStepNDArrayConverter implements NDArrayConverter {
        private Class<?> cFrom;
        private Class<?> cTo;
        private NDArrayConverter c1;
        private NDArrayConverter c2;

        @Override
        public boolean canConvert(NDArray from, NDArrayFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(NDArray from, Class<?> to) {
            return cFrom.isAssignableFrom(from.get().getClass()) && to.isAssignableFrom(cTo);
        }

        @Override
        public <T> T convert(NDArray from, NDArrayFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(NDArray from, Class<T> to) {
            NDArray sArr = NDArray.create(c1.convert(from, SerializedNDArray.class));
            return (T) c2.convert(sArr, cTo);
        }
    }
}
