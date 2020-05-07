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
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.format.*;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.nd4j.common.primitives.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ImageConverterRegistry extends AbstractRegistry<ImageConverter> {

    private static final ImageConverterRegistry INSTANCE = new ImageConverterRegistry();

    protected ImageConverterRegistry(){
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

    public static ImageConverter getConverterFor(Image img, Class<?> type ){
        return INSTANCE.getConverterForClass(img, type);
    }

    public static ImageConverter getConverterFor(Image img, ImageFormat<?> type ){
        return INSTANCE.getConverterForType(img, type);
    }

    public ImageConverter getConverterForClass(Image img, Class<?> type ){
        if(factories == null)
            init();

        if(factoriesMap.containsKey(type)){
            return factoriesMap.get(type).get(0);       //TODO multiple converters available
        }

        for(ImageConverter c : factories){
            if(c.canConvert(img, type)){
                return c;
            }
        }

        //No factory is available. Try to fall back on X -> PNG -> Y
        if(type != Png.class){
            ImageConverter c1 = getConverterForClass(img, Png.class);
            if(c1 != null){
                Image i2 = Image.create(c1.convert(img, Png.class));            //TODO this is ugly - we throw this result away!
                ImageConverter c2 = getConverterForClass(i2, type);
                return new TwoStepImageConverter(img.get().getClass(), type, c1, c2);
            }
        }

        return null;
    }

    public ImageConverter getConverterForType(Image img, ImageFormat<?> type ){
        if(factories == null)
            init();

        for(ImageConverter c : factories){
            if(c.canConvert(img, type)){
                return c;
            }
        }
        return null;
    }

    public static void addConverter(ImageConverter f){
        INSTANCE.addFactoryInstance(f);
    }

    @AllArgsConstructor
    private static class TwoStepImageConverter implements ImageConverter {
        private Class<?> cFrom;
        private Class<?> cTo;
        private ImageConverter c1;
        private ImageConverter c2;

        @Override
        public boolean canConvert(Image from, ImageFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(Image from, Class<?> to) {
            return cFrom.isAssignableFrom(from.get().getClass()) && to.isAssignableFrom(cTo);
        }

        @Override
        public <T> T convert(Image from, ImageFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(Image from, Class<T> to) {
            Image png = Image.create(c1.convert(from, Png.class));
            return (T) c2.convert(png, cTo);
        }
    }
}
