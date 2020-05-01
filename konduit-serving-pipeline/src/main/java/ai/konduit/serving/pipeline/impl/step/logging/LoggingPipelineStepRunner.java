package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoggingPipelineStepRunner implements PipelineStepRunner {

    private final LoggingPipelineStep step;

    @Override
    public void close() {
        //No-op
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Data data) {



        return data;
    }
}
