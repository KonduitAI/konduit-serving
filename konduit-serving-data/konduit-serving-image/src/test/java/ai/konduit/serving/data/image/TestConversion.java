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

package ai.konduit.serving.data.image;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Bmp;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.base.BaseImageFile;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestConversion {

    @Test
    public void testConversion(){
        /*
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$FrameToMatConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$MatToFrameConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$FrameToOpenCVMatConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$OpenCVMatToFrameConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$MatToOpenCVMatConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$OpenCVMatToMatConverter
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$MatToPng
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$PngToMat
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$FrameToPng
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$PngToFrame
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$OpenCVMatToPng
        ai.konduit.serving.data.javacv.format.JavaCVImageConverters$PngToOpenCVMat
         */

        File f = Resources.asFile("data/5_32x32.png");

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Mat mat = org.bytedeco.opencv.global.opencv_imgcodecs.imread(f.getAbsolutePath());
        Frame frame = converter.convert(mat);

        Image iMat = checkSize(Image.create(mat));
        Image iFrame = checkSize(Image.create(frame));

        //To Mat:
        Mat m1 = iMat.getAs(Mat.class);
        Mat m2 = iFrame.getAs(Mat.class);
        assertTrue(equalMats(m1, m2));

        //To Frame:
        Frame f1 = iMat.getAs(Frame.class);
        Frame f2 = iFrame.getAs(Frame.class);
        assertTrue(equalFrames(f1, f2));

        checkEncodingConversion(mat, frame, iMat, iFrame, Png.class);
        checkEncodingConversion(mat, frame, iMat, iFrame, Bmp.class);


        //Test Data
        for(Image i : new Image[]{iMat, iFrame}){
            Data d = Data.singleton("myImage", i);
            String json = d.toJson();
            Data dJson = Data.fromJson(json);

            assertEquals(d, dJson);
        }
    }

    private <T extends BaseImageFile> void checkEncodingConversion(Mat mat, Frame frame, Image iMat, Image iFrame, Class<T> type) {
        //To encoded:
        T p1 = iMat.getAs(type);
        T p2 = iFrame.getAs(type);
        assertTrue(equalImages(p1, p2));

        //From encoded:
        Image png = checkSize(Image.create(p1));
        Mat pm = png.getAs(Mat.class);
        Frame pf = png.getAs(Frame.class);
        assertTrue(equalMats(mat, pm));
        assertTrue(equalFrames(frame, pf));
    }

    public Image checkSize(Image i){
        assertEquals(32, i.height());
        assertEquals(32, i.width());
        return i;
    }

    protected static boolean equalMats(Mat m1, Mat m2){
        Png p1 = Image.create(m1).getAs(Png.class);
        Png p2 = Image.create(m2).getAs(Png.class);
        return equalImages(p1, p2);
    }

    protected static boolean equalFrames(Frame f1, Frame f2){
        Png p1 = Image.create(f1).getAs(Png.class);
        Png p2 = Image.create(f2).getAs(Png.class);
        return equalImages(p1, p2);
    }

    protected static boolean equalImages(BaseImageFile f1, BaseImageFile f2){
        try {
            BufferedImage bi1 = ImageIO.read(new ByteArrayInputStream(f1.getBytes()));
            BufferedImage bi2 = ImageIO.read(new ByteArrayInputStream(f2.getBytes()));
            return bufferedImagesEqual(bi1, bi2);
        } catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getHeight() != img2.getHeight() || img1.getWidth() != img2.getWidth()) {
            return false;
        }
        int w = img1.getWidth();
        int h = img1.getHeight();

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int rgb1 = img1.getRGB(i,j);
                int rgb2 = img2.getRGB(i,j);
                if (rgb1 != rgb2) {
                    return false;
                }
            }
        }
        return true;
    }

}
