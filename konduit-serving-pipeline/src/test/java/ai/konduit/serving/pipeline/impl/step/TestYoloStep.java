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

package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.bbox.yolo.YoloToBoundingBoxStep;
import ai.konduit.serving.pipeline.util.NDArrayUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TestYoloStep {

    @Test
    public void testYoloStep(){

        int mb = 1;
        int cl = 3;
        int b = 2;
        int h = 4;
        int w = 4;
        float threshold = 0.5f;

        //Add 2 detected objects: (0,1) for bb 0, and (3,2) for bb 1
        //Then random data for the rest
        Random r = new Random(12345);

        float[][][][][] f = new float[mb][b][cl+5][h][w];
        for( int i=0; i<b; i++ ){
            for(int x=0; x<w; x++ ){
                for( int y=0; y<h; y++ ){

                    if(x==0 && y==1){
                        f[0][0][0][y][x] = 0.7f;        //px, 0 to 1 in box
                        f[0][0][1][y][x] = 0.8f;        //py, 0 to 1 in box
                        f[0][0][2][y][x] = 2.0f;        //width, in grid units
                        f[0][0][3][y][x] = 1.5f;        //height, in grid units
                        f[0][0][4][y][x] = 0.9f;        //Confidence

                        f[0][0][5][y][x] = 0.75f;        //class 0 prob
                        f[0][0][6][y][x] = 0.125f;        //class 1 prob
                        f[0][0][7][y][x] = 0.125f;        //class 2 prob
                    } else if(x==3 && y==2){
                        f[0][1][0][y][x] = 0.2f;        //px, 0 to 1 in box
                        f[0][1][1][y][x] = 0.3f;        //py, 0 to 1 in box
                        f[0][1][2][y][x] = 3.5f;        //width, in grid units
                        f[0][1][3][y][x] = 0.75f;        //height, in grid units
                        f[0][1][4][y][x] = 0.8f;        //Confidence

                        f[0][1][5][y][x] = 0.25f;        //class 0 prob
                        f[0][1][6][y][x] = 0.25f;        //class 1 prob
                        f[0][1][7][y][x] = 0.5f;        //class 2 prob
                    } else {
                        //Below threshold
                        f[0][0][4][y][x] = r.nextFloat() * threshold;        //Confidence
                        f[0][1][4][y][x] = r.nextFloat() * threshold;        //Confidence
                    }
                }
            }
        }

        for(boolean nchw : new boolean[]{true, false}) {

            Pipeline p = SequencePipeline.builder()
                    .add(new YoloToBoundingBoxStep()
                            .classLabels("a", "b", "c")
                            .nchw(nchw))
                    .build();

            PipelineExecutor exec = p.executor();

            NDArray arr = NDArray.create(f);
            SerializedNDArray sa = arr.getAs(SerializedNDArray.class);
            sa = new SerializedNDArray(NDArrayType.FLOAT, new long[]{mb, b * (cl + 5), h, w}, sa.getBuffer());      //Reshape
            NDArray arr2 = NDArray.create(sa);

            if(!nchw) {
                float[][][][] fNchw = arr2.getAs(float[][][][].class);
                arr2 = NDArray.create(NDArrayUtils.nchwToNhwc(fNchw));
            }

            Data in = Data.singleton("in", arr2);
            Data out = exec.exec(in);

            //System.out.println(out.toJson());

            List<BoundingBox> expList = new ArrayList<>();
            expList.add(BoundingBox.create(0.7f / 4, (1.0f + 0.8f) / 4, 1.5f / 4, 2.0f / 4, "a", 0.75));
            expList.add(BoundingBox.create((3 + 0.2f) / 4, (2 + 0.3f) / 4, 0.75f / 4, 3.5f / 4, "c", 0.5));

            List<BoundingBox> actList = out.getListBoundingBox(YoloToBoundingBoxStep.DEFAULT_OUTPUT_NAME);
            assertEquals(expList, actList);


            String json = p.toJson();
            String yaml = p.toYaml();
            Pipeline pj = Pipeline.fromJson(json);
            Pipeline py = Pipeline.fromYaml(yaml);

            assertEquals(p, pj);
            assertEquals(p, py);

        }
    }
}
