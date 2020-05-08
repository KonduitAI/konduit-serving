package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import org.nd4j.common.base.Preconditions;

public class ImageToNDArrayStepRunnerFactory implements PipelineStepRunnerFactory {


    @Override
    public boolean canRun(PipelineStep pipelineStep) {
        return pipelineStep instanceof ImageToNDArrayStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep pipelineStep) {
        Preconditions.checkState(canRun(pipelineStep), "Unable to run pipeline step: %s", pipelineStep.getClass());
        return new ImageToNDArrayStepRunner((ImageToNDArrayStep) pipelineStep);
    }
}
