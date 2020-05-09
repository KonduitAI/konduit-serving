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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.exception.DataConversionException;
import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JavaImageConverters {

    private JavaImageConverters(){ }

    public static class IdentityConverter implements ImageConverter {

        @Override
        public boolean canConvert(Image from, ImageFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(Image from, Class<?> to) {
            return to.isAssignableFrom(from.get().getClass());
        }

        @Override
        public <T> T convert(Image from, ImageFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(Image from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert %s to %s", from.get().getClass(), to);
            return (T)from.get();
        }
    }

    @AllArgsConstructor
    public static abstract class BaseConverter implements ImageConverter {
        protected Class<?> cFrom;
        protected Class<?> cTo;

        @Override
        public boolean canConvert(Image from, ImageFormat<?> to) {
            return false;
        }

        @Override
        public boolean canConvert(Image from, Class<?> to) {
            return cFrom.isAssignableFrom(from.get().getClass()) && cTo.isAssignableFrom(to);
        }

        @Override
        public <T> T convert(Image from, ImageFormat<T> to) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public <T> T convert(Image from, Class<T> to) {
            Preconditions.checkState(canConvert(from, to), "Unable to convert image to format %s", to);
            return doConversion(from, to);
        }

        protected abstract <T> T doConversion(Image from, Class<T> to);
    }

    public static class PngToBufferedImageConverter extends BaseConverter {

        public PngToBufferedImageConverter() {
            super(Png.class, BufferedImage.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            Png png = (Png) from.get();
            byte[] bytes = png.getBytes();
            try(ByteArrayInputStream is = new ByteArrayInputStream(bytes)){
                BufferedImage bi = ImageIO.read(is);
                return (T) bi;
            } catch (IOException e){
                throw new DataLoadingException("Error converting PNG to BufferedImage", e);
            }
        }
    }

    public static class BufferedImageToPngConverter extends BaseConverter {
        public BufferedImageToPngConverter() {
            super(BufferedImage.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = (BufferedImage) from.get();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                ImageIO.write(bi, "png", os);
            } catch (IOException e){
                throw new DataConversionException("Error converting BufferedImage to PNG", e);
            }
            byte[] bytes = os.toByteArray();
            return (T) new Png(bytes);
        }
    }
}
