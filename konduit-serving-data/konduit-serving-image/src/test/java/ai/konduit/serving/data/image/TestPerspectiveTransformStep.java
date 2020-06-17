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

import ai.konduit.serving.data.image.step.point.perspective.convert.PerspectiveTransformStep;
import ai.konduit.serving.data.image.step.segmentation.index.DrawSegmentationStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TestPerspectiveTransformStep {

    @Test
    public void testNoReferenceWithTarget() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(PerspectiveTransformStep.builder()
                        .inputName("point")
                        .inputName("boundingBox")
                        .sourcePoints(Arrays.asList(
                                Point.create(3, 3),
                                Point.create(0, 3),
                                Point.create(3, 0),
                                Point.create(0, 0)
                        ))
                        .targetPoints(Arrays.asList(
                                Point.create(0, 0),
                                Point.create(3, 0),
                                Point.create(0, 3),
                                Point.create(3, 3)
                        ))
                        .build())
                .build();

        Data data = Data.empty();
        data.put("point", Point.create(2, 2, "bar", 0.3));
        data.put("boundingBox", BoundingBox.create(1, 1, 2, 2, "foo", 0.7));

        Data out = p.executor().exec(data);

        Point outPoint = out.getPoint("point");
        BoundingBox outBB = out.getBoundingBox("boundingBox");

        assertEquals(Point.create(1, 1, "bar", 0.3), outPoint);
        assertEquals(BoundingBox.create(2,2, 2, 2, "foo", 0.7), outBB);
    }

    @Test
    public void testNoReferenceNoTarget() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(PerspectiveTransformStep.builder()
                        .inputName("point")
                        .inputName("boundingBox")
                        .sourcePoints(Arrays.asList(
                                Point.create(4, 4),
                                Point.create(0, 4),
                                Point.create(4, 0),
                                Point.create(0, 0)
                        ))
                        .build())
                .build();

        Data data = Data.empty();
        data.put("point", Point.create(3, 3, "bar", 0.3));
        data.put("boundingBox", BoundingBox.create(1, 1, 2, 2, "foo", 0.7));

        Data out = p.executor().exec(data);

        Point outPoint = out.getPoint("point");
        BoundingBox outBB = out.getBoundingBox("boundingBox");

        assertEquals(Point.create(1, 1, "bar", 0.3), outPoint);
        assertEquals(BoundingBox.create(3,3, 2, 2, "foo", 0.7), outBB);
    }

    @Test
    public void testWithReferenceWithTarget() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(PerspectiveTransformStep.builder()
                        .inputName("point")
                        .inputName("boundingBox")
                        .sourcePoints(Arrays.asList(
                                Point.create(1, 0),
                                Point.create(2, 0),
                                Point.create(0, 3),
                                Point.create(3, 3)
                        ))
                        .targetPoints(Arrays.asList(
                                Point.create(0, 0),
                                Point.create(3, 0),
                                Point.create(0, 3),
                                Point.create(3, 3)
                        ))
                        .referenceHeight(3)
                        .referenceWidth(3)
                        .build())
                .build();

        Data data = Data.empty();
        data.put("point", Point.create(2, 2, "bar", 0.3));
        data.put("boundingBox", BoundingBox.create(1, 1, 2, 2, "foo", 0.7));

        Data out = p.executor().exec(data);

        Point outPoint = out.getPoint("point");
        BoundingBox outBB = out.getBoundingBox("boundingBox");

        assertEquals(Point.create(1, 1, "bar", 0.3), outPoint);
        assertEquals(BoundingBox.create(2,2, 2, 2, "foo", 0.7), outBB);
    }

    @Test
    public void testReferenceNoTarget() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(PerspectiveTransformStep.builder()
                        .inputName("point")
                        .inputName("boundingBox")
                        .sourcePoints(Arrays.asList(
                                Point.create(4, 4),
                                Point.create(0, 4),
                                Point.create(4, 0),
                                Point.create(0, 0)
                        ))
                        .build())
                .build();

        Data data = Data.empty();
        data.put("point", Point.create(3, 3, "bar", 0.3));
        data.put("boundingBox", BoundingBox.create(1, 1, 2, 2, "foo", 0.7));

        Data out = p.executor().exec(data);

        Point outPoint = out.getPoint("point");
        BoundingBox outBB = out.getBoundingBox("boundingBox");

        assertEquals(Point.create(1, 1, "bar", 0.3), outPoint);
        assertEquals(BoundingBox.create(3,3, 2, 2, "foo", 0.7), outBB);
    }

    @Ignore
    @Test
    public void testReferenceWithTargetImage() throws Exception {
        SequencePipeline.Builder b = SequencePipeline.builder()
                .add(DrawSegmentationStep.builder()
                        .image(null)
                        .segmentArray("class_idxs")
                        .outputName("out")
                        .classColors(Arrays.asList("red", "green", "blue"))
                        .build());

            b.add(PerspectiveTransformStep.builder()
                    .inputName("out")
                    .sourcePoints(Arrays.asList(
                            Point.create(3, 3),
                            Point.create(0, 3),
                            Point.create(3, 0),
                            Point.create(0, 0)
                    ))
                    .targetPoints(Arrays.asList(
                            Point.create(0, 0),
                            Point.create(3, 0),
                            Point.create(0, 3),
                            Point.create(3, 3)
                    ))
                    .referenceWidth(32)
                    .referenceHeight(32)
                    .build());

            b.add(ShowImagePipelineStep.builder()
                    .displayName("Segment")
                    .imageName("out")
                    .width(256)
                    .height(256)
                    .build());

        Pipeline p = b.build();

        //Segmentation indices array: [minibatch, height, width] - values are integers,
        INDArray arr = Nd4j.create(DataType.INT32, 1, 64, 64);                                              //Red background
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(16, 48), NDArrayIndex.interval(16, 48)).assign(1);     //Green square in middle
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(32, 64), NDArrayIndex.interval(32, 64)).assign(2);     //Blue square in bottom right

        Data d = Data.singleton("class_idxs", NDArray.create(arr));

        p.executor().exec(d);

        Thread.sleep(100000);
    }

    @Ignore
    @Test
    public void testReferenceImageWithTargetImage() throws Exception {
        SequencePipeline.Builder b = SequencePipeline.builder()
                .add(DrawSegmentationStep.builder()
                        .image(null)
                        .segmentArray("class_idxs")
                        .outputName("out")
                        .classColors(Arrays.asList("red", "green", "blue"))
                        .build());

        b.add(PerspectiveTransformStep.builder()
                .inputName("out")
                .sourcePoints(Arrays.asList(
                        Point.create(3, 3),
                        Point.create(0, 3),
                        Point.create(3, 0),
                        Point.create(0, 0)
                ))
                .targetPoints(Arrays.asList(
                        Point.create(0, 0),
                        Point.create(3, 0),
                        Point.create(0, 3),
                        Point.create(3, 3)
                ))
                .referenceImage("out")
                .build());

        b.add(ShowImagePipelineStep.builder()
                .displayName("Segment")
                .imageName("out")
                .width(256)
                .height(256)
                .build());

        Pipeline p = b.build();

        //Segmentation indices array: [minibatch, height, width] - values are integers,
        INDArray arr = Nd4j.create(DataType.INT32, 1, 64, 64);                                              //Red background
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(16, 48), NDArrayIndex.interval(16, 48)).assign(1);     //Green square in middle
        arr.get(NDArrayIndex.point(0), NDArrayIndex.interval(32, 64), NDArrayIndex.interval(32, 64)).assign(2);     //Blue square in bottom right

        Data d = Data.singleton("class_idxs", NDArray.create(arr));

        p.executor().exec(d);

        Thread.sleep(100000);
    }
}
