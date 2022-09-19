package ai.konduit.serving.documentparser;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;

public class DocumentParserRunnerFactory implements PipelineStepRunnerFactory {
    @Override
    public boolean canRun(PipelineStep step) {
        return step instanceof DocumentParserStep;
    }

    @Override
    public PipelineStepRunner create(PipelineStep step) {
        return new DocumentParserRunner((DocumentParserStep) step);
    }
}
