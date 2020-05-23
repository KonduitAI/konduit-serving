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
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Data;
import lombok.Getter;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * SequencePipeline is a simple stack of pipeline steps - the output of one is fed into the next
 *
 * @author Alex Black
 * @see GraphPipeline
 */
@Data
public class SequencePipeline implements Pipeline {

    @Getter
    private List<PipelineStep> steps;

    public SequencePipeline(@JsonProperty("steps") List<PipelineStep> steps) {
        this.steps = steps;
    }


    @Override
    public PipelineExecutor executor() {
        return new SequencePipelineExecutor(this);
    }

    @Override
    public int size() {
        return steps != null ? steps.size() : 0;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        protected List<PipelineStep> steps = new ArrayList<>();

        public Builder add(PipelineStep step){
            this.steps.add(step);
            return this;
        }

        public SequencePipeline build(){
            return new SequencePipeline(steps);
        }
    }


}
