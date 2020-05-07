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
import ai.konduit.serving.pipeline.impl.data.image.Png;
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
        org.opencv.core.Mat opencvMat = new org.opencv.core.Mat(mat.address());

        Image iMat = checkSize(Image.create(mat));
        Image iFrame = checkSize(Image.create(frame));
        Image iCVMat = checkSize(Image.create(opencvMat));

        //To Mat:
        Mat m1 = iMat.getAs(Mat.class);
        Mat m2 = iFrame.getAs(Mat.class);
        Mat m3 = iCVMat.getAs(Mat.class);
        assertTrue(equalMats(m1, m2));
        assertTrue(equalMats(m1, m3));

        //To Frame:
        Frame f1 = iMat.getAs(Frame.class);
        Frame f2 = iFrame.getAs(Frame.class);
        Frame f3 = iCVMat.getAs(Frame.class);
        assertTrue(equalFrames(f1, f2));
        assertTrue(equalFrames(f1, f3));

        //To OpenCVMat:
        org.opencv.core.Mat om1 = iMat.getAs(org.opencv.core.Mat.class);
        org.opencv.core.Mat om2 = iFrame.getAs(org.opencv.core.Mat.class);
        org.opencv.core.Mat om3 = iCVMat.getAs(org.opencv.core.Mat.class);
        assertTrue(equalOpenCvMats(om1, om2));
        assertTrue(equalOpenCvMats(om1, om3));



        //To PNG:
        Png p1 = iMat.getAs(Png.class);
        Png p2 = iFrame.getAs(Png.class);
        Png p3 = iCVMat.getAs(Png.class);
        assertTrue(equalPngs(p1, p2));
        assertTrue(equalPngs(p1, p3));

        //From PNG:
        Image png = checkSize(Image.create(p1));
        Mat pm = png.getAs(Mat.class);
        Frame pf = png.getAs(Frame.class);
        org.opencv.core.Mat pcvm = png.getAs(org.opencv.core.Mat.class);
        assertTrue(equalMats(mat, pm));
        assertTrue(equalFrames(frame, pf));
        assertTrue(equalOpenCvMats(opencvMat, pcvm));



        //Test Data
        for(Image i : new Image[]{iMat, iFrame, iCVMat}){
            Data d = Data.singleton("myImage", i);
            String json = d.toJson();
            Data dJson = Data.fromJson(json);

            assertEquals(d, dJson);
        }
    }

    public Image checkSize(Image i){
        assertEquals(32, i.height());
        assertEquals(32, i.width());
        return i;
    }

    protected static boolean equalMats(Mat m1, Mat m2){
        Png p1 = Image.create(m1).getAs(Png.class);
        Png p2 = Image.create(m2).getAs(Png.class);
        return equalPngs(p1, p2);
    }

    protected static boolean equalFrames(Frame f1, Frame f2){
        Png p1 = Image.create(f1).getAs(Png.class);
        Png p2 = Image.create(f2).getAs(Png.class);
        return equalPngs(p1, p2);
    }

    protected static boolean equalOpenCvMats(org.opencv.core.Mat m1, org.opencv.core.Mat m2){
        Png p1 = Image.create(m1).getAs(Png.class);
        Png p2 = Image.create(m2).getAs(Png.class);
        return equalPngs(p1, p2);
    }

    protected static boolean equalPngs(Png png1, Png png2){
        try {
            BufferedImage bi1 = ImageIO.read(new ByteArrayInputStream(png1.getBytes()));
            BufferedImage bi2 = ImageIO.read(new ByteArrayInputStream(png2.getBytes()));
            return bufferedImagesEqual(bi1, bi2);
        } catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    protected static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getHeight() != img2.getHeight() || img1.getWidth() != img2.getWidth()) {
            return false;
        }
        int w = img1.getWidth();
        int h = img1.getHeight();

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (img1.getRGB(i,j) != img2.getRGB(i,j)) {
                    return false;
                }
            }
        }
        return true;
    }

}
