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

package ai.konduit.serving.pipeline.impl.data.image;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import ai.konduit.serving.pipeline.registry.ImageConverterRegistry;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import java.util.Arrays;

@AllArgsConstructor
public abstract class BaseImage<T> implements Image {

    protected final T image;

    @Override
    public Object get() {
        return image;
    }

    @Override
    public <T> T getAs(ImageFormat<T> format) {
        return ImageConverterRegistry.getConverterFor(this, format).convert(this, format);
    }

    @Override
    public <T> T getAs(Class<T> type) {
        ImageConverter converter = ImageConverterRegistry.getConverterFor(this, type);
        Preconditions.checkState(converter != null, "No converter found for converting from %s to %s", image.getClass(), type);
        return converter.convert(this, type);
    }

    @Override
    public boolean canGetAs(ImageFormat<?> format) {
        ImageConverter converter = ImageConverterRegistry.getConverterFor(this, format);
        return converter != null;
    }

    @Override
    public boolean canGetAs(Class<?> type) {
        return false;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Image))
            return false;

        Image o2 = (Image)o;

        //TODO is this actually reliable for checks?
        Png png1 = getAs(Png.class);
        Png png2 = o2.getAs(Png.class);

        byte[] b1 = png1.getBytes();
        byte[] b2 = png2.getBytes();

        return Arrays.equals(b1, b2);
    }
}
