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

import ai.konduit.serving.data.image.step.bb.point.BoundingBoxToPointStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static ai.konduit.serving.data.image.step.bb.point.BoundingBoxToPointStep.ConversionMethod.*;
import static org.junit.Assert.assertEquals;

public class TestConvertBoundingBoxToPointStep {

    @Test
    public void testSingle() throws Exception {
        for (boolean outName : new boolean[]{false, true}) {
            for (boolean keepOthers : new boolean[]{false, true}) {
                for (BoundingBoxToPointStep.ConversionMethod method : new BoundingBoxToPointStep.ConversionMethod[]{BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT, CENTER})
                    for (boolean nullNames : new boolean[]{false, true}) {

                        Pipeline p = SequencePipeline.builder()
                                .add(BoundingBoxToPointStep.builder()
                                        .bboxName(nullNames ? null : "bbox")
                                        .outputName(outName ? null : "myOutput")
                                        .method(method)
                                        .keepOtherFields(keepOthers)
                                        .build())
                                .build();


                        BoundingBox bb = BoundingBox.createXY(1, 2, 3, 4, "label", 0.5);
                        Data in = Data.singleton("bbox", bb);
                        if (keepOthers) {
                            in.put("somekey", "somevalue");
                            in.put("otherkey", Math.PI);
                        }
                        Data out = p.executor().exec(in);

                        Point point = out.getPoint(outName ? "bbox" : "myOutput");
                        assertEquals(bb.label(), point.label());
                        assertEquals(bb.probability(), point.probability(), 0);

                        switch (method) {
                            case TOP_LEFT:
                                assertEquals(bb.x1(), point.x(), 0);
                                assertEquals(bb.y1(), point.y(), 0);
                                break;
                            case TOP_RIGHT:
                                assertEquals(bb.x2(), point.x(), 0);
                                assertEquals(bb.y1(), point.y(), 0);
                                break;
                            case BOTTOM_LEFT:
                                assertEquals(bb.x1(), point.x(), 0);
                                assertEquals(bb.y2(), point.y(), 0);
                                break;
                            case BOTTOM_RIGHT:
                                assertEquals(bb.x2(), point.x(), 0);
                                assertEquals(bb.y2(), point.y(), 0);
                                break;
                            case CENTER:
                                assertEquals(bb.cx(), point.x(), 0);
                                assertEquals(bb.cy(), point.y(), 0);
                                break;
                        }

                        if (keepOthers) {
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

    @Test
    public void testList() {
            for (boolean nullNames : new boolean[]{false, true}) {
                Pipeline p = SequencePipeline.builder()
                        .add(BoundingBoxToPointStep.builder()
                                .bboxName(nullNames ? null : "bbox")
                                .outputName("myOutput")
                                .method(TOP_LEFT)
                                .build())
                        .build();

                List<BoundingBox> lbb = Arrays.asList(BoundingBox.create(0.5, 0.22, 0.25, 0.3), BoundingBox.create(0.5, 0.22, 0.25, 0.3));
                Data in = Data.singletonList("bbox", lbb, ValueType.BOUNDING_BOX);
                Data out = p.executor().exec(in);

                List<Point> lOut = out.getListPoint("myOutput");
                assertEquals(lbb.size(), lOut.size());
                for (int i = 0; i < lbb.size(); i++) {
                    Point pOut = lOut.get(i);
                    BoundingBox bbIn = lbb.get(i);

                    assertEquals(bbIn.x1(), pOut.x(),0);
                    assertEquals(bbIn.y1(), pOut.y(),0);
                }
            }
        }
}
