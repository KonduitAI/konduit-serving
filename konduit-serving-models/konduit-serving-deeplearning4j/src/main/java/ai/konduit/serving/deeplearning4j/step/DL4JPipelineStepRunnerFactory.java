package ai.konduit.serving.deeplearning4j.step;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import org.nd4j.common.base.Preconditions;

public class DL4JPipelineStepRunnerFactory implements PipelineStepRunnerFactory {


    @Override
    public boolean canRun(PipelineStep pipelineStep) {
        return pipelineStep instanceof DL4JModelPipelineStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep pipelineStep) {
        Preconditions.checkState(canRun(pipelineStep), "Unable to run pipeline step: %s", pipelineStep.getClass());

        DL4JModelPipelineStep ps = (DL4JModelPipelineStep)pipelineStep;
        return new DL4JPipelineStepRunner(ps);
    }
}
