package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SequencePipeline implements Pipeline {

    @Getter
    private List<PipelineStep> steps;

    @Override
    public PipelineExecutor executor() {
        return new SequencePipelineExecutor(this);
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
