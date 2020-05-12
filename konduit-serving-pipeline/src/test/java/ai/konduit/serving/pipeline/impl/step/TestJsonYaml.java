/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestJsonYaml {

    @Test
    public void testJsonYamlSimple(){
        Pipeline p = SequencePipeline.builder()
                .add(LoggingPipelineStep.builder().build())
                .build();

        String json = p.toJson();
        Pipeline pJson = Pipeline.fromJson(json);
        assertEquals(p, pJson);


        String yaml = p.toYaml();
        Pipeline pYaml = Pipeline.fromYaml(yaml);
        assertEquals(p, pYaml);
    }

    @Test
    public void testJsonYamlSimpleGraph(){
        fail("Not yet re-implemented");
        Map<String, PipelineStep> m = Collections.singletonMap("logging", LoggingPipelineStep.builder().build());
        Pipeline p = new GraphPipeline(null, null);   //m);

        String json = p.toJson();
        Pipeline pJson = Pipeline.fromJson(json);
        assertEquals(p, pJson);


        String yaml = p.toYaml();
        Pipeline pYaml = Pipeline.fromYaml(yaml);
        assertEquals(p, pYaml);
    }


}
