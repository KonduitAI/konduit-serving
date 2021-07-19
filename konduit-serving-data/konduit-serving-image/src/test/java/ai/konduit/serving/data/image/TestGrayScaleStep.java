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

import ai.konduit.serving.data.image.step.grayscale.GrayScaleStep;
import ai.konduit.serving.data.image.step.resize.ImageResizeStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.File;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.junit.Assert.assertEquals;

public class TestGrayScaleStep {

    @Test
    public void testGrayScale() {
        File f = Resources.asFile("data/mona_lisa.png");
        Image i = Image.create(f);
        Mat input = i.getAs(Mat.class);
        Mat clone = input.clone();
        Pipeline p = SequencePipeline.builder()
                .add(new GrayScaleStep())
                .build();

        Data in = Data.singleton("img", i);
        Data out = p.executor().exec(in);

        Image outImg = out.getImage("img");
        Mat as = outImg.getAs(Mat.class);
        cvtColor(clone,clone, COLOR_BGR2GRAY);
        assertEquals(clone.channels(),as.channels());


    }


    @Test
    public void testGrayScale3Channels() throws Exception {
        File f = Resources.asFile("data/mona_lisa.png");
        Image i = Image.create(f);
        Mat input = i.getAs(Mat.class);
        Mat clone = input.clone();
        Pipeline p = SequencePipeline.builder()
                .add(new GrayScaleStep().outputChannels(3))
                .add(new ShowImageStep()
                        .displayName("Image Viewer")
                        .width(1024)
                        .height(1024)
                        .imageName("image")
                )
                .build();

        Data in = Data.singleton("image", i);
        Data out = p.executor().exec(in);

        Image outImg = out.getImage("image");
        Mat as = outImg.getAs(Mat.class);
        cvtColor(clone,clone, COLOR_BGR2GRAY);
        cvtColor(clone,clone, COLOR_GRAY2BGR);

        assertEquals(clone.channels(),as.channels());
        assertEquals(3,as.channels());

    }

}
