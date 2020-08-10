package ai.konduit.serving.models.onnx.step;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import org.nd4j.common.base.Preconditions;

public class ONNXPipelineStepRunnerFactory implements PipelineStepRunnerFactory {

    @Override
    public boolean canRun(PipelineStep step) {
        return step instanceof ONNXStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep step) {
        Preconditions.checkState(canRun(step), "Unable to run pipeline step: %s", step.getClass());
        return new ONNXRunner((ONNXStep) step);
    }
}
