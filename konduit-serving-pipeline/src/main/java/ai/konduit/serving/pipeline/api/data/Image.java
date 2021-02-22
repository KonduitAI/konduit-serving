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

package ai.konduit.serving.pipeline.api.data;

import ai.konduit.serving.pipeline.api.format.ImageFactory;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import ai.konduit.serving.pipeline.registry.ImageFactoryRegistry;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;

public interface Image {

    int height();

    int width();

    int channels();

    Object get();

    <T> T getAs(ImageFormat<T> format);

    <T> T getAs(Class<T> type);

    boolean canGetAs(ImageFormat<?> format);

    boolean canGetAs(Class<?> type);

    //TODO how will this work for PNG, JPG etc files?
    static Image create(@NonNull Object from) {
        if(from instanceof Image) {
            return (Image) from;
        }
        ImageFactory f = ImageFactoryRegistry.getFactoryFor(from);
        Preconditions.checkState(f != null, "Unable to create Image from object of %s - no ImageFactory instances" +
                " are available that can convert this type to Konduit Serving Image", from.getClass());

        return f.create(from);
    }

    static boolean canCreateFrom(@NonNull Object from){
        return ImageFactoryRegistry.getFactoryFor(from) != null;
    }

}
