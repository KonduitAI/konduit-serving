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

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.step.bb.extract.ExtractBoundingBoxStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;
import org.nd4j.common.primitives.Pair;
import org.nd4j.common.resources.Resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestExtractBBStep {

    @Test
    public void testExtractSimple() throws Exception {

        File f = Resources.asFile("data/mona_lisa.png");

        int oHW = 64;

        for(boolean outName : new boolean[]{false, true}) {
            for(boolean keepOthers : new boolean[]{false, true}) {
                for (boolean outHW : new boolean[]{true, false}) {
                    for (boolean nullNames : new boolean[]{false, true}) {

                        Pipeline p = SequencePipeline.builder()
                                /*
                                .add(ShowImagePipelineStep.builder()
                                        .imageName("image")
                                        .displayName("Original")
                                        .height(897)
                                        .width(600)
                                        .build())
                                 */
                                .add(new ExtractBoundingBoxStep()
                                        .imageName(nullNames ? null : "image")
                                        .bboxName(nullNames ? null : "bbox")
                                        .aspectRatio(1.0)
                                        .resizeH(outHW ? oHW : null)
                                        .resizeW(outHW ? oHW : null)
                                        .outputName(outName ? null : "myOutput")
                                        )
                                /*
                                .add(ShowImagePipelineStep.builder()
                                        .imageName("image")
                                        .height(0)
                                        .width(0)
                                        .displayName("Cropped image")
                                        .build())
                                 */
                                .build();


                        Data in = Data.singleton("image", Image.create(f));
                        in.put("bbox", BoundingBox.create(0.5, 0.22, 0.25, 0.3));
                        if(keepOthers){
                            in.put("somekey", "somevalue");
                            in.put("otherkey", Math.PI);
                        }

                        Data out = p.executor().exec(in);

                        Image img = out.getImage(outName ? "image" : "myOutput");
                        if (outHW) {
                            assertEquals(oHW, img.height());
                            assertEquals(oHW, img.width());
                        } else {
                            assertEquals(1077, img.height());
                            assertEquals(721, img.width());
                        }

                        if(keepOthers){
                            assertEquals("somevalue", out.getString("somekey"));
                            assertEquals(Math.PI, out.getDouble("otherkey"), 0.0);
                        }

                        String json = p.toJson();
                        Pipeline p2 = Pipeline.fromJson(json);

                        assertEquals(p, p2);
                    }
                }
            }
        }
    }

    @Test
    public void testList(){
        File f = Resources.asFile("data/mona_lisa.png");

        int oHW = 64;

        for(boolean outHW : new boolean[]{true, false}) {
            for (boolean nullNames : new boolean[]{false, true}) {
                Pipeline p = SequencePipeline.builder()
                        .add(new ExtractBoundingBoxStep()
                                .imageName(nullNames ? null : "image")
                                .bboxName(nullNames ? null : "bbox")
                                .aspectRatio(1.0)
                                .resizeH(outHW ? oHW : null)
                                .resizeW(outHW ? oHW : null)
                                )
                        .build();


                Data in = Data.singleton("image", Image.create(f));
                List<BoundingBox> lbb = Arrays.asList(BoundingBox.create(0.5, 0.22, 0.25, 0.3), BoundingBox.create(0.5, 0.22, 0.25, 0.3));
                in.putListBoundingBox("bbox", lbb);

                Data out = p.executor().exec(in);

                List<Image> lOut = out.getListImage("image");
                assertEquals(2, lOut.size());
                for(Image img : lOut) {
                    if (outHW) {
                        assertEquals(oHW, img.height());
                        assertEquals(oHW, img.width());
                    } else {
                        assertEquals(1077, img.height());
                        assertEquals(721, img.width());
                    }
                }
            }
        }
    }

    @Test
    public void testWithImageToNDArrayConfig(){

        //Make sure we account for the crop region when extracting

        int inH = 32;
        int inW = 48;

        BufferedImage bi = new BufferedImage(inW, inH, BufferedImage.TYPE_INT_RGB);
        for( int i=0; i<inH; i++ ){
            for( int j=0; j<inW; j++ ){
                int r = i;
                int g = j;
                int b = 100 + i;
                int rgb = r << 16 | g << 8 | b;
                bi.setRGB(j, i, rgb);
            }
        }


        List<Pair<BoundingBox,Pair<Integer,Integer>>> lbb = Arrays.asList(
                Pair.of(BoundingBox.createXY(0.0, 1.0, 0.0, 1.0), Pair.of(32, 48)),           //[32,48]
                Pair.of(BoundingBox.createXY(0.0, 1.0, 0.0, 1.0), Pair.of(32, 32)),           //[32,32]
                Pair.of(BoundingBox.createXY(0.25, 0.75, 0.25, 0.75), Pair.of(16, 16))            //[16,16]
        );

        for(Pair<BoundingBox,Pair<Integer,Integer>> pair: lbb){
            System.out.println(pair);

            BoundingBox bb = pair.getFirst();
            int oH = pair.getSecond().getFirst();
            int oW = pair.getSecond().getSecond();

            Data in = Data.singleton("image", Image.create(bi));
            in.put("bbox", bb);

            ImageToNDArrayConfig conf = new ImageToNDArrayConfig()
                    .height(oH)
                    .width(oW);
            Pipeline p = SequencePipeline.builder()
                    .add(new ExtractBoundingBoxStep()
                            .imageToNDArrayConfig(conf))
                    .build();

            PipelineExecutor exec = p.executor();

            Data out = exec.exec(in);

            //Work out section of original image that the BB represents, within the crop

            BoundingBox cropRegion = ImageToNDArray.getCropRegion(Image.create(bi), conf);

            int cropWidth = (int) (inW * cropRegion.width());
            int cropHeight = (int) (inH * cropRegion.height());

            //Convert BB coordinates to original image coordinates, accounting for (a) original crop, (b) BB within crop
            //Crop region top left, in pixels of original image
            int cX1 = (int) (inW * cropRegion.x1());
            int cY1 = (int) (inH * cropRegion.y1());
            //Bounding box within crop region, in pipels of original image
            int pX1 = cX1 + (int) (cropWidth * bb.x1());
            int pX2 = cX1 + (int) (cropWidth * bb.x2());
            int pY1 = cY1 + (int) (cropHeight * bb.y1());
            int pY2 = cY1 + (int) (cropHeight * bb.y2());

            System.out.println(pX1 + "," + pX2 + "," + pY1 + "," + pY2);

            Image outImg = out.getImage("image");
            BufferedImage biOut = outImg.getAs(BufferedImage.class);

            assertEquals(oH, biOut.getHeight());
            assertEquals(oW, biOut.getWidth());

            for( int x=0; x<oW; x++ ){
                for( int y=0; y<oH; y++ ){
                    int origX = pX1 + x;
                    int origY = pY1 + y;

                    int rgbOrig = bi.getRGB(origX, origY);
                    int rgbExtracted = biOut.getRGB(x, y);

                    /*
                    int rOrig = (rgbOrig & 0b111111110000000000000000) >> 16;       //i
                    int gOrig = (rgbOrig & 0b000000001111111100000000) >> 8;        //j
                    int bOrig = rgbOrig & 0b000000000000000011111111;               //100 + i

                    int rE = (rgbExtracted & 0b111111110000000000000000) >> 16;       //i
                    int gE = (rgbExtracted & 0b000000001111111100000000) >> 8;        //j
                    int bE = rgbExtracted & 0b000000000000000011111111;               //100 + i
                     */

                    String s = x + "," + y;
                    assertEquals(s, rgbOrig, rgbExtracted);
                }
            }
        }
    }
}
