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

package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.image.PngImage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static org.junit.Assert.*;

public class ImageTests {

    @Test
    public void testBasicLoadingConversion() throws Exception {

        File f = Resources.asFile("data/5_32x32.png");
        System.out.println(f.getAbsolutePath());

        Image i = Image.create(f);
        assertTrue(i instanceof PngImage);
        assertTrue(i.get() instanceof Png);

        Png p = i.getAs(Png.class);

        byte[] bytes = p.getBytes();
        byte[] expBytes;
        try(InputStream is = new BufferedInputStream(new FileInputStream(f))){
            expBytes = IOUtils.toByteArray(is);
        }

        assertArrayEquals(expBytes, bytes);


        //Data and JSON serialization
        Data d = Data.singleton("myImage", i);
        String json = d.toJson();
        System.out.println(json);
        Data dJson = Data.fromJson(json);
        assertEquals(d, dJson);


        //PNG -> BufferedImage
        BufferedImage bi = i.getAs(BufferedImage.class);
        BufferedImage biExp = ImageIO.read(f);
        boolean eq = bufferedImagesEqual(biExp, bi);
        assertTrue("BufferedImage instances should be equal", eq);

        //BufferedImage creation, Data, JSON and BufferedImage -> PNG
        Image i2 = Image.create(bi);
        Data d2 = Data.singleton("myImage", i2);
        String json2 = d2.toJson();
        Data d2Json = Data.fromJson(json2);

        assertEquals(d2, d2Json);

        Png png2 = d2Json.getImage("myImage").getAs(Png.class);
        //assertEquals(p, png2);        //TODO - this fails - but that doesn't necessarily mean it's a different image given byte[]
        boolean eq2 = equalPngs(p, png2);
        assertTrue("Images differ after conversion to/from PNG", eq2);
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
