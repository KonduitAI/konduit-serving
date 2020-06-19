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

import ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep;
import ai.konduit.serving.data.image.step.grid.crop.CropGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawGridStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestGridSteps {

    @Test
    @Ignore   //To be run manually
    public void testDrawGridStep() throws Exception {

        File f = Resources.asFile("data/mona_lisa.png");
        Image i = Image.create(f);

        Data in = Data.singleton("image", i);
        in.putListDouble("x", Arrays.asList(0.5, 0.7, 0.2, 0.6));
        in.putListDouble("y", Arrays.asList(0.2, 0.3, 0.6, 0.7));

        in.putListPoint("points", Arrays.asList(Point.create(0.5, 0.2), Point.create(0.7, 0.3), Point.create(0.2, 0.6), Point.create(0.6, 0.7)));


        Pipeline p = SequencePipeline.builder()
                .add(DrawGridStep.builder()
                        .borderColor("green")
                        .gridColor("blue")
                        .coordsArePixels(false)
                        .gridX(3)
                        .gridY(10)
                        .pointsName("points")
                        .imageName("image")
                        .borderThickness(10)
                        .gridThickness(4)
                        .build())
                .add(new ShowImagePipelineStep("image", "Display", null, null, false))
                .build();

        PipelineExecutor exec = p.executor();

        exec.exec(in);

        Thread.sleep(1000000);
    }

    protected void assertApproxEqual(Image i1, Image i2, int differenceThreshold, int maxPixelsDifferent) {
        BufferedImage bi1 = i1.getAs(BufferedImage.class);
        BufferedImage bi2 = i2.getAs(BufferedImage.class);

        int countDifferent = 0;
        int h = bi1.getHeight();
        int w = bi2.getWidth();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int rgb1 = bi1.getRGB(j, i);
                int rgb2 = bi2.getRGB(j, i);

                int r1 = (rgb1 & 0b111111110000000000000000) >> 16;
                int g1 = (rgb1 & 0b000000001111111100000000) >> 8;
                int b1 = rgb1 & 0b000000000000000011111111;
                int r2 = (rgb1 & 0b111111110000000000000000) >> 16;
                int g2 = (rgb1 & 0b000000001111111100000000) >> 8;
                int b2 = rgb1 & 0b000000000000000011111111;

                int difference = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);

                if (difference > differenceThreshold) {
                    countDifferent++;
                }
            }
        }

        String msg = countDifferent + " of " + (h * w) + " pixels differ by more than theshold=" + differenceThreshold;
        assertTrue(msg, countDifferent < maxPixelsDifferent);
    }


    @Test
    public void testCropGridStep() throws Exception {

        File f = Resources.asFile("data/shelves.png");
        Image i = Image.create(f);



        int gx = 4;
        int gy = 2;

        for(boolean fixed : new boolean[]{false, true}) {
        for (boolean px : new boolean[]{false, true}) {
            Data in = Data.singleton("image", i);
            List<Point> points;
            if (px) {
                points = Arrays.asList(Point.create(0.015 * i.width(), 0.33 * i.height()), Point.create(0.04 * i.width(), 0.70 * i.height()),
                        Point.create(0.815 * i.width(), 0.42 * i.height()), Point.create(0.795 * i.width(), 0.83 * i.height()));
            } else {
                points = Arrays.asList(Point.create(0.015, 0.33), Point.create(0.04, 0.70), Point.create(0.815, 0.42), Point.create(0.795, 0.83));
            }
            in.put("key", "value");
            if(!fixed){
                in.putListPoint("points", points);
            }

            for(boolean outName : new boolean[]{false, true}) {
                for (boolean keep : new boolean[]{false, true}) {
                    for(boolean bb : new boolean[]{false, true}) {
                        PipelineStep s;
                        PipelineStep draw;
                        if(fixed){
                            s = CropFixedGridStep.builder()
                                    .coordsArePixels(false)
                                    .points(points)
                                    .gridX(gx)
                                    .gridY(gy)
                                    .imageName("image")
                                    .keepOtherFields(keep)
                                    .outputName(outName ? "output" : null)
                                    .boundingBoxName(bb ? "bbox" : null)
                                    .coordsArePixels(px)
                                    //.aspectRatio(1.0)
                                    .build();

                            draw = DrawFixedGridStep.builder()
                                    .borderColor("green")
                                    .gridColor("blue")
                                    .coordsArePixels(px)
                                    .gridX(gx)
                                    .gridY(gy)
                                    .points(points)
                                    .imageName("image")
                                    .borderThickness(4)
                                    .gridThickness(2)
                                    .build();
                        } else {
                            s = CropGridStep.builder()
                                    .coordsArePixels(false)
                                    .gridX(gx)
                                    .gridY(gy)
                                    .pointsName("points")
                                    .imageName("image")
                                    .keepOtherFields(keep)
                                    .outputName(outName ? "output" : null)
                                    .boundingBoxName(bb ? "bbox" : null)
                                    .coordsArePixels(px)
                                    //.aspectRatio(1.0)
                                    .build();

                            draw = DrawGridStep.builder()
                                    .borderColor("green")
                                    .gridColor("blue")
                                    .coordsArePixels(px)
                                    .gridX(gx)
                                    .gridY(gy)
                                    .pointsName("points")
                                    .imageName("image")
                                    .borderThickness(4)
                                    .gridThickness(2)
                                    .build();
                        }


                            Pipeline p = SequencePipeline.builder()
                                    .add(draw)
                                    //.add(new ShowImagePipelineStep().imageName("image").height(null).width(null))
                                    .add(s)
                                    //.add(new ShowImagePipelineStep().imageName(outName ? "output" : CropGridStep.DEFAULT_OUTPUT_NAME).displayName("Display").width(null).height(null).allowMultiple(true))
                                    .build();

                            PipelineExecutor exec = p.executor();

                            Data out = exec.exec(in);
                            List<Image> l = out.getListImage(outName ? "output" : CropGridStep.DEFAULT_OUTPUT_NAME);
                            assertEquals(gx * gy, l.size());

                            if (keep) {
                                assertTrue(out.has("key"));
                                assertEquals("value", out.getString("key"));
                            } else {
                                assertFalse(out.has("key"));
                            }

                            if (bb) {
                                assertTrue(out.has("bbox"));
                                List<BoundingBox> lbb = out.getListBoundingBox("bbox");
                                assertEquals(gx * gy, lbb.size());
                            } else {
                                assertFalse(out.has("bbox"));
                            }

                            String json = p.toJson();
//                            System.out.println(json);
                            Pipeline p2 = Pipeline.fromJson(json);
                            assertEquals(p, p2);

//                        Thread.sleep(1000000);
                        }
                    }
                }
            }
        }

//        Thread.sleep(1000000);
    }
}
