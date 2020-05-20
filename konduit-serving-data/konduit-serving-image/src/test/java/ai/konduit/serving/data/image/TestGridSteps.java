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

import ai.konduit.serving.data.image.step.grid.draw.DrawGridStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.File;
import java.util.Arrays;

public class TestGridSteps {

    @Test @Ignore   //To be run manually
    public void testDrawGridStep() throws Exception {

        File f = Resources.asFile("data/mona_lisa.png");
        Image i = Image.create(f);

        Data in = Data.singleton("image", i);
        in.putListDouble("x", Arrays.asList(0.5, 0.7, 0.2, 0.6));
        in.putListDouble("y", Arrays.asList(0.2, 0.3, 0.6, 0.7));


        Pipeline p = SequencePipeline.builder()
                .add(DrawGridStep.builder()
                        .borderColor("green")
                        .gridColor("blue")
                        .coordsArePixels(false)
                        .grid1(3)
                        .grid2(10)
                        .xName("x")
                        .yName("y")
                        .imageName("image")
                        .borderThickness(10)
                        .gridThickness(4)
                    .build())
                .add(new ShowImagePipelineStep("image", "Display", null, null))
                .build();

        PipelineExecutor exec = p.executor();

        exec.exec(in);

        Thread.sleep(1000000);
    }

    @Test
    public void testOrders(){
        //The order in which we provide the points should make zero difference - except for the grid1 / grid2 order
        int numOrders = 4 * 3 * 2;

        int[][] orders = new int[numOrders][4];
        int x=0;
        for( int i=0; i<4; i++ ){
            for( int j=0; j<4; j++ ){
                if(j == i)
                    continue;
                for( int k=0; k<4; k++ ){
                    if(k == i || k == j)
                        continue;

                    orders[x][0] = i;
                    orders[x][1] = j;
                    orders[x][2] = k;
                    for( int l=0; l<4; l++ ){
                        if(i != l && j != l && k != l){
                            orders[x][3] = l;
                            break;
                        }
                    }

                    System.out.println(x + " - " + Arrays.toString(orders[x]));
                    x++;
                }
            }
        }



    }

}
