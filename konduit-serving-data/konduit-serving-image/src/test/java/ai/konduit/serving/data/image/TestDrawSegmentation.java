/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;
import org.nd4j.common.resources.Resources;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.File;
import java.util.Arrays;

public class TestDrawSegmentation {

    private final boolean show = false;   //Set to true to visualize

    @Test
    public void testDrawSegment() throws Exception {
        SequencePipeline.Builder b = SequencePipeline.builder()
                .add( new DrawSegmentationStep()
                        .image(null)
                        .segmentArray("class_idxs")
                        .outputName("out")
                        .classColors(Arrays.asList("red", "green", "blue"))
                        );

        if(show) {
                b.add(new ShowImageStep()
                    .displayName("Segment")
                    .imageName("out")
                    .width(256)
                    .height(256)
                    );
        }
        Pipeline p = b.build();

        //Segmentation indices array: [minibatch, height, width] - values are integers,
        INDArray arr = Nd4j.create(DataType.INT32, 1, 64, 64);                                              //Red background
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(16, 48), NDArrayIndex.interval(16, 48)).assign(1);     //Green square in middle
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(32, 64), NDArrayIndex.interval(32, 64)).assign(2);     //Blue square in bottom right

        Data d = Data.singleton("class_idxs", NDArray.create(arr));

        p.executor().exec(d);

        if(show)
            Thread.sleep(100000);
    }

    @Test
    public void testDrawSegmentOpacity() throws Exception {
        SequencePipeline.Builder b = SequencePipeline.builder()
                .add(new DrawSegmentationStep()
                        .image("image")
                        .segmentArray("class_idxs")
                        .outputName("out")
                        .classColors(Arrays.asList("red", "green"))     //2 colors for 3 classes -> should auto generate the 3rd
                        .opacity(0.5)
                        .backgroundClass(0)     //Don't draw background color if this is set
                        );
        if (show) {
            b.add( new ShowImageStep()
                    .displayName("Segment")
                    .imageName("out")
                    .width(535)
                    .height(800)
                    );
        }
        Pipeline p = b.build();

        File f = Resources.asFile("data/mona_lisa_535x800.png");
        Image i = Image.create(f);

        //Segmentation indices array: [minibatch, height, width] - values are integers,
        INDArray arr = Nd4j.create(DataType.INT32, 1, 800, 535);                                            //Red background
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(300, 600), NDArrayIndex.interval(200, 500)).assign(1);     //Green square in middle
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(500, 800), NDArrayIndex.interval(300, 535)).assign(2);     //Blue square in bottom right

        Data d = Data.singleton("class_idxs", NDArray.create(arr));
        d.put("image", i);

        p.executor().exec(d);

        if(show)
            Thread.sleep(100000);
    }
}
