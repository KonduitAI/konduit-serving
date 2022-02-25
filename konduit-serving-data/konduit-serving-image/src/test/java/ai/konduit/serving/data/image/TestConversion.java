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

package ai.konduit.serving.data.image;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Bmp;
import ai.konduit.serving.pipeline.impl.data.image.Jpeg;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.base.BaseImageFile;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.resources.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestConversion {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();


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



    @Test
    public void testBufferedImageConversion() throws Exception {
        //https://github.com/KonduitAI/konduit-serving/issues/426
        //https://github.com/KonduitAI/konduit-serving/issues/424

        String[] format = new String[]{"bi", "png", "jpg", "mat"};

        File dir = testDir.newFolder();

        Random r = new Random(12345);
        for(boolean alpha : new boolean[]{false, true}) {
            for (String from : format) {
                if(alpha && "jpg".equals(from))
                    continue;

                Image in;
                switch (from){
                    case "bi":
                        in = Image.create(randomBI(r, alpha));
                        break;
                    case "png":
                        File fPng = new File(dir, "test.png");
                        ImageIO.write(randomBI(r, alpha), "png", fPng);
                        in = Image.create(fPng);
                        break;
                    case "jpg":
                        File fjpg = new File(dir, "test.jpg");
                        ImageIO.write(randomBI(r, alpha), "jpg", fjpg);
                        in = Image.create(fjpg);
                        break;
                    case "mat":
                        File fPng2 = new File(dir, "test2.png");
                        ImageIO.write(randomBI(r, alpha), "png", fPng2);
                        in = Image.create(org.bytedeco.opencv.global.opencv_imgcodecs.imread(fPng2.getAbsolutePath()));
                        break;
                    default:
                        throw new RuntimeException();
                }

                for (String to : format) {
                    switch (to){
                        case "bi":
                            in.getAs(BufferedImage.class);
                            break;
                        case "png":
                            in.getAs(Png.class);
                            break;
                        case "jpg":
                            in.getAs(Jpeg.class);
                            break;
                        case "mat":
                            in.getAs(Mat.class);
                            break;
                    }
                }
            }
        }
    }

    protected BufferedImage randomBI(Random r, boolean alpha){
        BufferedImage bi = new BufferedImage(32, 32, alpha ? BufferedImage.TYPE_4BYTE_ABGR: BufferedImage.TYPE_3BYTE_BGR);
        for( int i=0; i<32; i++ ){
            for( int j=0; j<32; j++ ){
                bi.setRGB(i, j, rgb(r, alpha));
            }
        }
        return bi;
    }

    private static int rgb(Random rng, boolean alpha){
        int r = rng.nextInt(255);
        int g = rng.nextInt(255);
        int b = rng.nextInt(255);
        int rgb = r << 16 | g << 8 | b;
        if(alpha){
            int a = rng.nextInt(255);
            rgb |= a << 24;
        }
        return rgb;
    }
}
