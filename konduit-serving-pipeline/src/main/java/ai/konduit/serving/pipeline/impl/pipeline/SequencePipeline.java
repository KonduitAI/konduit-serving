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

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SequencePipeline is a simple stack of pipeline steps - the output of one is fed into the next
 *
 * @author Alex Black
 * @see GraphPipeline
 */
@Data
@Accessors(fluent = true)
@Schema(description = "A type of pipeline that defines the execution flow in a series of configurable steps.")
@JsonName("SEQUENCE_PIPELINE")
public class SequencePipeline implements Pipeline {

    @Getter
    @Schema(description = "A list/sequence of configurable steps that determines the way a pipeline is executed.")
    private List<PipelineStep> steps;

    @Schema(description = "A unique identifier that's used to differentiate among different executing pipelines. Used " +
            "for identifying a pipeline while reporting metrics.")
    @EqualsAndHashCode.Exclude
    private String id;

    public SequencePipeline(@JsonProperty("steps") List<PipelineStep> steps, @JsonProperty("id") String id) {
        this.steps = steps;
        this.id = id;
    }


    @Override
    public PipelineExecutor executor() {
        return new SequencePipelineExecutor(this);
    }

    @Override
    public int size() {
        return steps != null ? steps.size() : 0;
    }

    @Override
    public String id() {
        if(id == null)
            id = UUID.randomUUID().toString().substring(0, 8);
        return id;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        protected List<PipelineStep> steps = new ArrayList<>();
        private String id;

        public Builder add(PipelineStep step){
            this.steps.add(step);
            return this;
        }

        public Builder id(String id){
            this.id = id;
            return this;
        }

        public SequencePipeline build(){
            return new SequencePipeline(steps, id);
        }
    }


}
