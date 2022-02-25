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

import ai.konduit.serving.data.image.step.point.draw.DrawPointsStep;
import ai.konduit.serving.data.image.step.show.ShowImageStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class TestDrawPointsStep {

    @Ignore
    @Test
    public void testNoReferenceImage() throws Exception {
        HashMap<String, String> colorMap = new HashMap<>();
        colorMap.put("foo", "red");
        colorMap.put("bar", "green");

        Pipeline p = SequencePipeline.builder()
                .add(new DrawPointsStep()
                        .classColors(colorMap)
                        .points(Collections.singletonList("points"))
                        .width(100)
                        .height(200)
                        .outputName("image"))
                .add(new ShowImageStep())
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
}
