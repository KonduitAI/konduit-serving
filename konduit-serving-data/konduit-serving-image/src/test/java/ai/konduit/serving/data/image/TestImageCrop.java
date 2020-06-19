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

import ai.konduit.serving.data.image.step.crop.ImageCropStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class TestImageCrop {

    @Test
    public void testSimple() throws Exception {

        File f = Resources.asFile("data/mona_lisa_535x800.png");

        for (boolean list : new boolean[]{false, true}) {
            for (boolean px : new boolean[]{false, true}) {
                for (String type : new String[]{"bbox", "points", "data_bbox", "data_points"}) {
                    for (int i = 0; i < 5; i++) {
                        String strCase = "Pixels = " + px + ", type = " + type + ", case = ";
                    /*
                    Test cases:
                    0 - Full image region, 0.0 to 1.0 (out == in)
                    1 - Internal crop
                    2 - Crop region partly outside image (top left corner)
                    3 - Crop region partly outside image (bottom right corner)
                    4 - Crop region entirely outside image (expect black output)
                     */

                        Image img = Image.create(f);
                        Data in;
                        if (list) {
                            in = Data.singletonList("image", Collections.singletonList(img), ValueType.IMAGE);
                        } else {
                            in = Data.singleton("image", img);
                        }

                        ImageCropStep s = new ImageCropStep();
                        s.coordsArePixels(px);

                        double x1, x2, y1, y2;
                        switch (i) {
                            case 0:
                                x1 = 0.0;
                                x2 = 1.0;
                                y1 = 0.0;
                                y2 = 1.0;
                                strCase += "Full image";
                                break;
                            case 1:
                                x1 = 0.2;
                                x2 = 0.8;
                                y1 = 0.2;
                                y2 = 0.8;
                                strCase += "Partial image (inside)";
                                break;
                            case 2:
                                x1 = -0.3;
                                x2 = 0.3;
                                y1 = -0.3;
                                y2 = 0.3;
                                strCase += "Partially outside (top left)";
                                break;
                            case 3:
                                x1 = 0.7;
                                x2 = 1.3;
                                y1 = 0.7;
                                y2 = 1.3;
                                strCase += "Partially outside (bottom right)";
                                break;
                            case 4:
                                x1 = 1.1;
                                x2 = 1.5;
                                y1 = 1.1;
                                y2 = 1.5;
                                strCase += "Totally outside";
                                break;
                            default:
                                throw new RuntimeException();
                        }

                        if (px) {
                            x1 *= img.width();
                            x2 *= img.width();
                            y1 *= img.height();
                            y2 *= img.height();
                        }

                        if (i == 0) {
                            if (px) {
                                x2--;
                                y2--;
                            } else {
                                x2 -= 1.0 / img.width();
                                y2 -= 1.0 / img.height();
                            }
                        }

                        switch (type) {
                            case "bbox":
                                s.cropBox(BoundingBox.createXY(x1, x2, y1, y2));
                                break;
                            case "points":
                                s.cropPoints(Arrays.asList(Point.create(x1, y1), Point.create(x2, y2)));
                                break;
                            case "data_bbox":
                                s.cropName("bbox");
                                in.put("bbox", BoundingBox.createXY(x1, x2, y1, y2));
                                break;
                            case "data_points":
                                s.cropName("points");
                                in.putListPoint("points", Arrays.asList(Point.create(x1, y1), Point.create(x2, y2)));
                                break;
                            default:
                                throw new RuntimeException();
                        }

                        Pipeline p = SequencePipeline.builder()
                                .add(s)
                                //.add(new ShowImagePipelineStep().displayName(strCase))
                                .build();

                        PipelineExecutor exec = p.executor();

                        exec.exec(in);
                        //Thread.sleep(200);
                    }
                }
            }
        }
    }
}
