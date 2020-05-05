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

import ai.konduit.serving.pipeline.api.format.FormatFactory;
import ai.konduit.serving.pipeline.api.format.ImageFactory;
import lombok.NonNull;

import java.util.*;

public abstract class BaseFactoryRegistry<T extends FormatFactory> extends AbstractRegistry<T> {

    //Intentionally package private (no/default access modifier)

    BaseFactoryRegistry(Class<T> factoryClass){
        super(factoryClass);
    }

    public boolean acceptFactory(T factory, Object o){
        return factory.canCreateFrom(o);
    }

    public Set<Class<?>> supportedForFactory(T factory){
        return factory.supportedTypes();
    }
}
