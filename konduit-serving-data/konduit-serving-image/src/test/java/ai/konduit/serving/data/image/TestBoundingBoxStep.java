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
import ai.konduit.serving.data.image.step.show.ShowImageStep;
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

public class TestBoundingBoxStep {

    @Test @Ignore   //To be run manually
    public void testBBox() throws Exception {

        File f = Resources.asFile("data/5_32x32.png");
        Image i = Image.create(f);

        Data in = Data.singleton("image", i);
//        in.put("bbox", BoundingBox.createXY(0.2, 0.5, 0.3, 0.9));
        in.putListBoundingBox("bbox", Arrays.asList(
                BoundingBox.createXY(0.2, 0.5, 0.3, 0.9, "red", null),
                BoundingBox.createXY(0.5, 0.8, 0.1, 0.5, "green", null)));

        Map<String,String> classColors = new HashMap<>();
        classColors.put("red", "rgb(255,0,0)");
        classColors.put("green", "rgb(0,255,0)");


        Pipeline p = SequencePipeline.builder()
                .add(new DrawBoundingBoxStep()
                        .imageName("image")
                        .bboxName("bbox")
                        .color("rgb(0,0,255)")
//                        .scale(DrawBoundingBoxStep.Scale.AT_LEAST)
//                        .resizeH(256)
//                        .resizeW(256)
                        .lineThickness(1)
                        .classColors(classColors)

                    )
                .add(new ShowImageStep("image", "Display", 256, 256, false))
                .build();

        PipelineExecutor exec = p.executor();

        exec.exec(in);

        Thread.sleep(100000);
    }

}
