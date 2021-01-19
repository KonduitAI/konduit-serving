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

package ai.konduit.serving.pipeline.registry;

import lombok.NonNull;

import java.util.*;

public abstract class AbstractRegistry<T> {

    protected Class<T> factoryClass;
    protected List<T> factories;
    protected Map<Class<?>,List<T>> factoriesMap;

    //Intentionally package private (no/default access modifier)

    AbstractRegistry(Class<T> factoryClass){
        this.factoryClass = factoryClass;
    }


    public int registryNumFactories(){
        if(factories == null)
            init();

        return factories.size();
    }

    public List<T> registryGetFactories(){
        if(factories == null)
            init();

        return Collections.unmodifiableList(factories);
    }

    public T registryGetFactoryFor(@NonNull Object o){
        if(factories == null)
            init();

        List<T> l = factoriesMap.get(o.getClass());
        if(l != null && !l.isEmpty())
            return l.get(0);    //TODO what if if there are multiple factories that can create an NDArray from this? Which should we use?

        //Otherwise: iterate through (for example: in case of interface)
        for(T f : factories ){
            if(acceptFactory(f, o))
                return f;
        }
        return null;
    }

    public abstract boolean acceptFactory(T factory, Object o);

    public abstract Set<Class<?>> supportedForFactory(T factory);

    protected synchronized void init(){
        if(factories != null)
            return;

        List<T> l = new ArrayList<>();
        Map<Class<?>,List<T>> m = new HashMap<>();

        ServiceLoader<T> sl = ServiceLoader.load(factoryClass);
        for (T f : sl) {
            l.add(f);
            Set<Class<?>> s = supportedForFactory(f);
            for (Class<?> c : s) {
                m.computeIfAbsent(c, x -> new ArrayList<>()).add(f);
            }
        }

        factoriesMap = m;
        factories = l;
    }

    public void addFactoryInstance(T factory){
        if(factories == null)
            init();
        this.factories.add(factory);
    }
}
