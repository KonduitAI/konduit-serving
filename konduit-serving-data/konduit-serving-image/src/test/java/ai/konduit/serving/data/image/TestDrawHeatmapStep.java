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

import ai.konduit.serving.data.image.step.point.heatmap.DrawHeatmapStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

public class TestDrawHeatmapStep {

    @Ignore
    @Test
    public void testSingleStep() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(DrawHeatmapStep.builder()
                        .point("points")
                        .width(100)
                        .height(200)
                        .outputName("image")
                        .build())
                .add(new ShowImagePipelineStep())
                .build();

        Data data = Data.empty();
        data.putListPoint("points",
                Arrays.asList(
                        Point.create(2, 2, "bar", 0.1),
                        Point.create(0.1, 0.1, "foo", 0.2),
                        Point.create(0.5, 0.8, "bar", 0.3),
                        Point.create(30, 59, "foo", 0.4)
                )
        );

        Data out = p.executor().exec(data);

        Thread.sleep(Long.MAX_VALUE);
    }

    @Ignore
    @Test
    public void testMultiStep() throws Exception {
        Pipeline p = SequencePipeline.builder()
                .add(DrawHeatmapStep.builder()
                        .point("points")
                        .width(100)
                        .height(200)
                        .outputName("image")
                        .radius(10)
                        .build())
                .add(new ShowImagePipelineStep())
                .build();

        Data data = Data.empty();
        data.putListPoint("points",
                Arrays.asList(
                        Point.create(2, 2, "bar", 0.1),
                        Point.create(0.1, 0.1, "foo", 0.2),
                        Point.create(0.5, 0.8, "bar", 0.3),
                        Point.create(30, 59, "foo", 0.4)
                )
        );

        PipelineExecutor executor = p.executor();
        Data out = executor.exec(data);

        executor.exec(Data.singletonList("points",
                Arrays.asList(
                        Point.create(8, 12, "bar", 0.1),
                        Point.create(0.2, 0.2, "foo", 0.2),
                        Point.create(0.6, 0.8, "bar", 0.3)
                ), ValueType.POINT));

        executor.exec(Data.singletonList("points",
                Arrays.asList(
                        Point.create(0.3, 0.3, "foo", 0.2),
                        Point.create(0.7, 0.8, "bar", 0.3)
                ), ValueType.POINT));

        executor.exec(Data.singletonList("points",
                Arrays.asList(
                        Point.create(0.9, 0.8, "bar", 0.3)
                ), ValueType.POINT));

        Thread.sleep(Long.MAX_VALUE);
    }

}
