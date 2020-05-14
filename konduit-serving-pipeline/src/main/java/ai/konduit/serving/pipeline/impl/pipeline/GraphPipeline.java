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
package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.StandardGraphStep;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * A pipeline with a graph structure - possibly including conditional operations, etc
 *
 * TODO THIS IS A PLACEHOLDER
 */
@Data
public class GraphPipeline implements Pipeline {
    public static final String INPUT_KEY = "input";

    private final Map<String, GraphStep> steps;
    private final String outputStepName;

    public GraphPipeline(@JsonProperty("steps") Map<String, GraphStep> steps, String outputStepName){
        //TODO JSON needs rewriting here...
        this.steps = steps;
        this.outputStepName = outputStepName;
    }

    @Override
    public PipelineExecutor executor() {
        return new GraphPipelineExecutor(this);
    }

    @Override
    public int size() {
        return steps != null ? steps.size() : 0;
    }
}
