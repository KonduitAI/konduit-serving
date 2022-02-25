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

package ai.konduit.serving.pipeline.impl.format;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.exception.DataConversionException;
import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import ai.konduit.serving.pipeline.api.format.ImageConverter;
import ai.konduit.serving.pipeline.api.format.ImageFormat;
import ai.konduit.serving.pipeline.impl.data.image.Bmp;
import ai.konduit.serving.pipeline.impl.data.image.Gif;
import ai.konduit.serving.pipeline.impl.data.image.Jpeg;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.base.BaseImageFile;
import lombok.AllArgsConstructor;
import org.nd4j.common.base.Preconditions;

import javax.imageio.ImageIO;
import java.awt.*;
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

    public static abstract class BaseToBufferedImageConverter<F extends BaseImageFile> extends BaseConverter {
        public BaseToBufferedImageConverter(Class<F> c) {
            super(c, BufferedImage.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            F f = (F) from.get();
            byte[] bytes = f.getBytes();
            try(ByteArrayInputStream is = new ByteArrayInputStream(bytes)){
                BufferedImage bi = ImageIO.read(is);
                return (T) bi;
            } catch (IOException e){
                throw new DataLoadingException("Error converting " + cFrom.getClass().getSimpleName() + " to BufferedImage", e);
            }
        }
    }

    public static class JpegToBufferedImageConverter extends BaseToBufferedImageConverter<Jpeg> {
        public JpegToBufferedImageConverter() {
            super(Jpeg.class);
        }
    }

    public static class PngToBufferedImageConverter extends BaseToBufferedImageConverter<Png> {
        public PngToBufferedImageConverter() {
            super(Png.class);
        }
    }

    public static class BmpToBufferedImageConverter extends BaseToBufferedImageConverter<Bmp> {
        public BmpToBufferedImageConverter() {
            super(Bmp.class);
        }
    }

    public static class GifToBufferedImageConverter extends BaseToBufferedImageConverter<Gif> {
        public GifToBufferedImageConverter() {
            super(Gif.class);
        }
    }

    public static abstract class BaseBufferedImageToOtherConverter<ToFormat extends BaseImageFile> extends BaseConverter {
        public BaseBufferedImageToOtherConverter(Class<ToFormat> to) {
            super(BufferedImage.class, to);
        }

        protected abstract String formatName();

        protected abstract ToFormat get(byte[] bytes);

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = (BufferedImage) from.get();
            bi = removeAlpha(bi);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                ImageIO.write(bi, formatName(), os);
            } catch (IOException e){
                throw new DataConversionException("Error converting BufferedImage to " + formatName(), e);
            }
            byte[] bytes = os.toByteArray();
            return (T) get(bytes);
        }
    }

    public static class BufferedImageToPngConverter extends BaseBufferedImageToOtherConverter<Png> {
        public BufferedImageToPngConverter() {
            super(Png.class);
        }

        @Override
        protected String formatName() {
            return "png";
        }

        @Override
        protected Png get(byte[] bytes) {
            return new Png(bytes);
        }
    }

    public static class BufferedImageToJpgConverter extends BaseBufferedImageToOtherConverter<Jpeg> {
        public BufferedImageToJpgConverter() {
            super(Jpeg.class);
        }

        @Override
        protected String formatName() {
            return "jpg";
        }

        @Override
        protected Jpeg get(byte[] bytes) {
            return new Jpeg(bytes);
        }
    }

    public static class BufferedImageToBmpConverter extends BaseBufferedImageToOtherConverter<Bmp> {
        public BufferedImageToBmpConverter() {
            super(Bmp.class);
        }

        @Override
        protected String formatName() {
            return "bmp";
        }

        @Override
        protected Bmp get(byte[] bytes) {
            return new Bmp(bytes);
        }
    }

    public static class BufferedImageToGifConverter extends BaseBufferedImageToOtherConverter<Gif> {
        public BufferedImageToGifConverter() {
            super(Gif.class);
        }

        @Override
        protected String formatName() {
            return "gif";
        }

        @Override
        protected Gif get(byte[] bytes) {
            return new Gif(bytes);
        }
    }

    public static class JpegToPngImageConverter extends BaseConverter {

        public JpegToPngImageConverter() {
            super(Jpeg.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Png g = Image.create(bi).getAs(Png.class);
            return (T) g;
        }
    }

    public static class PngToJpegConverter extends BaseConverter {
        public PngToJpegConverter() {
            super(Png.class, Jpeg.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Jpeg j = Image.create(bi).getAs(Jpeg.class);
            return (T) j;
        }
    }

    public static class BmpToPngImageConverter extends BaseConverter {

        public BmpToPngImageConverter() {
            super(Bmp.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Png g = Image.create(bi).getAs(Png.class);
            return (T) g;
        }
    }

    public static class PngToBmpConverter extends BaseConverter {
        public PngToBmpConverter() {
            super(Png.class, Bmp.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Bmp j = Image.create(bi).getAs(Bmp.class);
            return (T) j;
        }
    }

    public static class GifToPngImageConverter extends BaseConverter {

        public GifToPngImageConverter() {
            super(Gif.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Png g = Image.create(bi).getAs(Png.class);
            return (T) g;
        }
    }

    public static class PngToGifConverter extends BaseConverter {
        public PngToGifConverter() {
            super(Png.class, Gif.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BufferedImage bi = from.getAs(BufferedImage.class);
            Gif j = Image.create(bi).getAs(Gif.class);
            return (T) j;
        }
    }

    public static BufferedImage removeAlpha(BufferedImage in){
        if(!in.getColorModel().hasAlpha())
            return in;

        BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, in.getWidth(), in.getHeight());
        g.drawImage(in, 0, 0, null);
        g.dispose();

        return out;
    }
}
