package ai.konduit.serving.tensorrt;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;

public class TensorRTRunnerFactory implements PipelineStepRunnerFactory {
    @Override
    public boolean canRun(PipelineStep step) {
        return step instanceof TensorRTStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep step) {
        return new TensorRTRunner((TensorRTStep) step);
    }
}
