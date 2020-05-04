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

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import lombok.NonNull;
import org.nd4j.common.primitives.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ImageConverteRegistry extends AbstractRegistry<ImageConverter> {

    private static final ImageConverteRegistry INSTANCE = new ImageConverteRegistry();

    protected ImageConverteRegistry(){
        super(ImageConverter.class);
    }

    public static int numFactories(){
        return INSTANCE.registryNumFactories();
    }

    public static List<ImageConverter> getFactories(){
        return INSTANCE.registryGetFactories();
    }

    public static ImageConverter getFactoryFor(@NonNull Object o){
        return INSTANCE.registryGetFactoryFor(o);
    }

    @Override
    public boolean acceptFactory(ImageConverter factory, Object o) {
        Pair<Image, ImageFormat> p = (Pair<Image, ImageFormat>) o;
        return factory.canConvert(p.getFirst(), p.getSecond());
    }

    @Override
    public Set<Class<?>> supportedForFactory(ImageConverter factory) {
        return Collections.emptySet();
    }
}
