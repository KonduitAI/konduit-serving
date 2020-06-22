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

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.step.resize.ImageResizeStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestImageResize {

    @Test
    public void testWithAspectRatioHandling() throws Exception {

        for (AspectRatioHandling h : AspectRatioHandling.values()) {
            for (boolean wLarger : new boolean[]{false, true}) {
                String s = h + " - wLarger=" + wLarger;
                System.out.println(s);

                int[][][] chw;
                if (wLarger) {
                    chw = new int[3][32][48];
                } else {
                    chw = new int[3][48][32];
                }

                for (int c = 0; c < chw.length; c++) {
                    for (int y = 0; y < chw[0].length; y++) {
                        for (int x = 0; x < chw[0][0].length; x++) {
                            chw[c][y][x] = c + y + x;
                        }
                    }
                }

                Image i = Image.create(TestImageToNDArray.toBufferedImage(chw));


                Pipeline p = SequencePipeline.builder()
                        .add(new ImageResizeStep()
                                .aspectRatioHandling(h)
                                .height(32)
                                .width(32))
//                        .add(new ShowImagePipelineStep().displayName(s))
                        .build();

                Data in = Data.singleton("img", i);
                Data out = p.executor().exec(in);

                Image outImg = out.getImage("img");
                assertEquals(32, outImg.height());
                assertEquals(32, outImg.width());


                if (h == AspectRatioHandling.CENTER_CROP) {
                    int[][][] sub = new int[3][32][32];
                    if (wLarger) {
                        for (int c = 0; c < sub.length; c++) {
                            for (int y = 0; y < sub[0].length; y++) {
                                for (int x = 0; x < sub[0][0].length; x++) {
                                    sub[c][y][x] = chw[c][y][x + 8];
                                }
                            }
                        }
                    } else {
                        for (int c = 0; c < sub.length; c++) {
                            for (int y = 0; y < sub[0].length; y++) {
                                for (int x = 0; x < sub[0][0].length; x++) {
                                    sub[c][y][x] = chw[c][y + 8][x];
                                }
                            }
                        }
                    }

                    int[][][] act = toCHW(outImg.getAs(BufferedImage.class));
                    for( int c=0; c<3; c++ ){
                        for( int y=0; y<32; y++ ){
                            boolean eq = Arrays.equals(sub[c][y], act[c][y]);
                            if(!eq){
                                System.out.println(c + " - " + y);
                            }
                            assertTrue(eq);
                        }
                    }

//                    assertTrue(eq);
                }
                //TODO PAD and STRETCH validation
            }
        }
    }

    public int[][][] toCHW(BufferedImage bi){
        int[][][] out = new int[3][bi.getHeight()][bi.getWidth()];
        for( int y=0; y<bi.getHeight(); y++){
            for( int x=0; x<bi.getWidth(); x++ ){
                int rgb = bi.getRGB(x, y);
                int r = (rgb & 0b111111110000000000000000) >> 16;
                int g = (rgb & 0b000000001111111100000000) >> 8;
                int b = rgb & 0b000000000000000011111111;

                out[0][y][x] = r;
                out[1][y][x] = g;
                out[2][y][x] = b;
            }
        }
        return out;
    }

    @Ignore
    @Test
    public void testManual() throws Exception {

        File f = Resources.asFile("data/mona_lisa.png");;
        Data d = Data.singleton("image", Image.create(f));

        Pipeline p = SequencePipeline.builder()
                .add(new ImageResizeStep()
                        .aspectRatioHandling(AspectRatioHandling.CENTER_CROP)
                        .height(1024)
                        .width(768))
                        .add(new ShowImagePipelineStep())
                .build();

        p.executor().exec(d);

        Thread.sleep(Long.MAX_VALUE);
    }
}
