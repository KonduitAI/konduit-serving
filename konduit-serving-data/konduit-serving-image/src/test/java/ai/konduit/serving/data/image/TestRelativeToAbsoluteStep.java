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

import ai.konduit.serving.data.image.step.point.convert.RelativeToAbsoluteStep;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestRelativeToAbsoluteStep {

    @Test
    public void testRelativeToAbsolute(){


        Data d = Data.empty();
        d.put("pt", Point.create(0.3, 0.4));
        d.putListPoint("ptList", Arrays.asList(Point.create(0.3, 0.4)));
        d.put("box", BoundingBox.create(0.5, 0.6, 0.2, 0.3, "myLabel", 0.99));
        d.putListBoundingBox("boxList", Arrays.asList(BoundingBox.create(0.5, 0.6, 0.2, 0.3, "myLabel", 0.99)));

        Pipeline p = SequencePipeline.builder()
                .add(new RelativeToAbsoluteStep().imageH(200).imageW(300))
                .build();


        PipelineExecutor exec = p.executor();
        Data out = exec.exec(d);


        Point exp = Point.create(0.3*300, 0.4*200);
        BoundingBox bb = BoundingBox.create(0.5*300, 0.6*200, 0.2*200, 0.3*300, "myLabel", 0.99);

        assertEquals(exp, out.getPoint("pt"));
        assertEquals(bb, out.getBoundingBox("box"));

        assertEquals(Collections.singletonList(exp), out.getListPoint("ptList"));
        assertEquals(bb, out.getListBoundingBox("boxList").get(0));


        String json = p.toJson();
        String yaml = p.toYaml();
        Pipeline pj = Pipeline.fromJson(json);
        Pipeline py = Pipeline.fromYaml(yaml);

        assertEquals(p, pj);
        assertEquals(p, py);
    }

}
