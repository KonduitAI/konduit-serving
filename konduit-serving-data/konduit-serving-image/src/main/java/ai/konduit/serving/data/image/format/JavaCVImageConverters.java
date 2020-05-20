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

package ai.konduit.serving.data.image.format;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.exception.DataConversionException;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.format.JavaImageConverters;
import ai.konduit.serving.pipeline.util.FileUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class JavaCVImageConverters {

    private JavaCVImageConverters(){ }

    public static class FrameToMatConverter extends JavaImageConverters.BaseConverter {
        protected OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        public FrameToMatConverter() {
            super(Frame.class, Mat.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            Frame f = (Frame) from.get();
            Mat m = converter.convert(f);
            return (T)m;
        }
    }

    public static class MatToFrameConverter extends JavaImageConverters.BaseConverter {
        protected OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        public MatToFrameConverter() {
            super(Mat.class, Frame.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            Mat m = (Mat) from.get();
            Frame f = converter.convert(m);
            return (T)f;
        }
    }

    public static class MatToPng extends JavaImageConverters.BaseConverter {
        public MatToPng() {
            super(Mat.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            //TODO It may be possible to do this without the temp file
            Mat m = (Mat) from.get();
            File tempDir = FileUtils.getTempFileDir("konduit-serving-javacv");
            File f = new File(tempDir, UUID.randomUUID().toString() + ".png");
            String path = f.getAbsolutePath();

            org.bytedeco.opencv.global.opencv_imgcodecs.imwrite(path, m);
            try {
                byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(f);
                f.delete();
                return (T) new Png(bytes);
            } catch (IOException e){
                throw new DataConversionException("Error connverting Mat to Png", e);
            }
        }
    }

    public static class PngToMat extends JavaImageConverters.BaseConverter {
        public PngToMat() {
            super(Png.class, Mat.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            //TODO is there a way to do this without the temp file?
            Png p = (Png) from.get();
            byte[] bytes = p.getBytes();
            File tempDir = FileUtils.getTempFileDir("konduit-serving-javacv");
            File f = new File(tempDir, UUID.randomUUID().toString() + ".png");
            Mat mat;
            try {
                org.apache.commons.io.FileUtils.writeByteArrayToFile(f, bytes);
                mat = org.bytedeco.opencv.global.opencv_imgcodecs.imread(f.getAbsolutePath());
            } catch (IOException e){
                throw new DataConversionException("Error writing to temporary file for Png->Mat conversion", e);
            }
            f.delete();
            return (T) mat;
        }
    }

    public static class FrameToPng extends JavaImageConverters.BaseConverter {
        public FrameToPng() {
            super(Frame.class, Png.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            //Frame -> Mat -> Png. Is there a more efficient way?
            Frame f = (Frame) from.get();
            Mat m = Image.create(f).getAs(Mat.class);
            return (T) Image.create(m).getAs(Png.class);
        }
    }

    public static class PngToFrame extends JavaImageConverters.BaseConverter {
        public PngToFrame() {
            super(Png.class, Frame.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            //Png -> Mat -> Frame. Is there a more efficient way?
            Png p = (Png)from.get();
            Mat m = Image.create(p).getAs(Mat.class);
            return (T) Image.create(m).getAs(Frame.class);
        }
    }
}
