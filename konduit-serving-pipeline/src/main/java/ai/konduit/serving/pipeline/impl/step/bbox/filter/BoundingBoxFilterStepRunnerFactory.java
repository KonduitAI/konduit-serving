package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import org.nd4j.common.base.Preconditions;

public class BoundingBoxFilterStepRunnerFactory implements PipelineStepRunnerFactory {

    @Override
    public boolean canRun(PipelineStep step) {
        return step instanceof BoundingBoxFilterStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep step) {
        Preconditions.checkState(canRun(step), "Unable to run step: %s", step);
        return new BoundingBoxFilterStepRunner((BoundingBoxFilterStep) step);
    }
}
