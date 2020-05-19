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

import ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep;
import ai.konduit.serving.data.image.step.grid.draw.DrawGridStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
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
import java.util.HashMap;
import java.util.Map;

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
//                        .borderColor("yellow")
//                        .gridColor("blue")
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


    }

}
