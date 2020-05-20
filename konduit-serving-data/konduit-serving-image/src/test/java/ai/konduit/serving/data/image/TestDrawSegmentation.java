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

import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.Arrays;

public class TestDrawSegmentation {

    @Test
    @Ignore
    public void testDrawSegment() throws Exception {


        Pipeline p = SequencePipeline.builder()
                .add(DrawSegmentationStep.builder()
                        .image(null)
                        .segmentArray("class_idxs")
                        .outputName("out")
                        .classColors(Arrays.asList("red", "green", "blue"))
                        .build())
                .add(ShowImagePipelineStep.builder()
                        .displayName("Segment")
                        .imageName("out")
                        .width(256)
                        .height(256)
                        .build())
                .build();

        //Segmentation indices array: [minibatch, height, width] - values are integers,
        INDArray arr = Nd4j.create(DataType.INT32, 1, 64, 64);                                              //Red background
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(16, 48), NDArrayIndex.interval(16, 48)).assign(1);     //Green square in middle
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(32, 64), NDArrayIndex.interval(32, 64)).assign(2);     //Blue square in bottom right

        Data d = Data.singleton("class_idxs", NDArray.create(arr));

        p.executor().exec(d);

        Thread.sleep(100000);
    }

}
