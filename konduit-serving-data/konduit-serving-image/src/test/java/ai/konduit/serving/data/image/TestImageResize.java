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
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestImageResize {

    @Test
    public void test() {

    }


    @Test
    public void testAspectRatioHandling() {

        for (AspectRatioHandling h : AspectRatioHandling.values()) {
            for (boolean wLarger : new boolean[]{false, true}) {
                System.out.println(h + " - wLarger=" + wLarger);

                int[][][] chw;
                if (wLarger) {
                    chw = new int[3][32][48];
                } else {
                    chw = new int[3][48][32];
                }

                for (int c = 0; c < chw.length; c++) {
                    for (int y = 0; y < chw[0].length; y++) {
                        for (int x = 0; x < chw[0][0].length; x++) {
                            chw[c][y][x] = 100 * c + y + x;
                        }
                    }
                }

                Image i = Image.create(TestImageToNDArray.toBufferedImage(chw));


                Pipeline p = SequencePipeline.builder()
                        .add(new ImageResizeStep()
                                .aspectRatioHandling(h)
                                .height(32)
                                .width(32))
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
                    BufferedImage bi = TestImageToNDArray.toBufferedImage(sub);
                    boolean eq = TestConversion.bufferedImagesEqual(outImg.getAs(BufferedImage.class), bi);
                    assertTrue(eq);
                }
                //TODO PAD and STRETCH
            }

        }

    }

}
