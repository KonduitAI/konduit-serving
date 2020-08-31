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
import ai.konduit.serving.pipeline.impl.data.image.Bmp;
import ai.konduit.serving.pipeline.impl.data.image.Jpeg;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.base.BaseImageFile;
import ai.konduit.serving.pipeline.impl.format.JavaImageConverters;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.CvArr;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

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

    public static abstract class OpenCVMatToAnyConverter extends JavaImageConverters.BaseConverter {
        final String ext;
        public OpenCVMatToAnyConverter(Class<?> other, String ext) {
            super(Mat.class, other);
            this.ext = ext;
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            Mat m = (Mat) from.get();
            BytePointer out = new BytePointer();
            org.bytedeco.opencv.global.opencv_imgcodecs.imencode(ext, m, out);

            out.position(0);
            return fromByteBuffer(out.asByteBuffer());
        }

        protected abstract <T> T fromByteBuffer(ByteBuffer byteBuffer);
    }

    public static class OpenCVAnyToMatConverter extends JavaImageConverters.BaseConverter {

        public OpenCVAnyToMatConverter(Class<?> other) {
            super(other, Mat.class);
        }

        @Override
        protected <T> T doConversion(Image from, Class<T> to) {
            BaseImageFile p = (BaseImageFile) from.get();
            ByteBuffer fileBytes = p.getFileBytes();
            fileBytes.position(0);

            Mat m = new Mat(new BytePointer(fileBytes), false);
            Mat out = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(m, opencv_imgcodecs.IMREAD_UNCHANGED);
            Mat ret = out.clone();
            //strip alpha channel if exists
            if(out.channels() > 3) {
                cvtColor(out,ret,CV_BGRA2BGR);
            }
            return (T) ret;
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

    public static class MatToPng extends OpenCVMatToAnyConverter {
        public MatToPng() {
            super(Png.class, ".png");
        }

        @Override
        protected <T> T fromByteBuffer(ByteBuffer byteBuffer) {
            return (T) new Png(byteBuffer);
        }
    }

    public static class MatToJpeg extends OpenCVMatToAnyConverter {
        public MatToJpeg() {
            super(Jpeg.class, ".jpg");
        }

        @Override
        protected <T> T fromByteBuffer(ByteBuffer byteBuffer) {
            return (T) new Jpeg(byteBuffer);
        }
    }
    public static class MatToBmp extends OpenCVMatToAnyConverter {
        public MatToBmp() {
            super(Bmp.class, ".bmp");
        }

        @Override
        protected <T> T fromByteBuffer(ByteBuffer byteBuffer) {
            return (T) new Bmp(byteBuffer);
        }
    }

    public static class PngToMat extends OpenCVAnyToMatConverter { public PngToMat() {
        super(Png.class);
    }}
    public static class JpegToMat extends OpenCVAnyToMatConverter { public JpegToMat() {
        super(Jpeg.class);
    }}
    public static class BmpToMat extends OpenCVAnyToMatConverter { public BmpToMat() {
        super(Bmp.class);
    }}

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
