package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;

public class LoggingPipelineStepRunnerFactory implements PipelineStepRunnerFactory {
    @Override
    public boolean canRun(PipelineStep pipelineStep) {
        return pipelineStep.getClass() == LoggingPipelineStep.class;
    }

    @Override
    public PipelineStepRunner create(@NonNull PipelineStep pipelineStep) {
        Preconditions.checkArgument(canRun(pipelineStep), "Unable to execute pipeline step of type: {}", pipelineStep.getClass());
        return new LoggingPipelineStepRunner((LoggingPipelineStep) pipelineStep);
    }
}
