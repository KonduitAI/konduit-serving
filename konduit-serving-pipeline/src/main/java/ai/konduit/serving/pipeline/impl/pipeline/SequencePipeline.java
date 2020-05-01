package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Getter;

import java.util.List;

public class SequencePipeline implements Pipeline {

    @Getter
    private List<PipelineStep> steps;

    @Override
    public PipelineExecutor executor() {
        return new SequencePipelineExecutor(this);
    }
}
