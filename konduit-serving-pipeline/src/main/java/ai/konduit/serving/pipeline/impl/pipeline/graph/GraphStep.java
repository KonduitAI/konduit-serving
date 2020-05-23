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

package ai.konduit.serving.pipeline.impl.pipeline.graph;

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.serde.GraphStepDeserializer;
import ai.konduit.serving.pipeline.impl.pipeline.serde.GraphStepSerializer;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using = GraphStepSerializer.class)
@JsonDeserialize(using = GraphStepDeserializer.class)
public interface GraphStep extends TextConfig {

    String name();

    void name(String name);

    GraphBuilder builder();

    String input();

    List<String> inputs();

    boolean hasStep();

    PipelineStep getStep();


    default GraphStep then(String name, PipelineStep step) {
        GraphStep s = new PipelineGraphStep(builder(), step, name, this.name());;
        builder().add(s);
        return s;
    }


    default GraphStep mergeWith(String name, GraphStep... steps) {
        List<String> allSteps = new ArrayList<>();
        allSteps.add(this.name());
        for(GraphStep g : steps){
            allSteps.add(g.name());
        }

        MergeStep out = new MergeStep(builder(), allSteps, name);
        builder().add(out);
        return out;
    }

}
