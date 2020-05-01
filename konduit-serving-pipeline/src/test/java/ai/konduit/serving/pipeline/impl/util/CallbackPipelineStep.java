package ai.konduit.serving.pipeline.impl.util;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.Factories;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public class CallbackPipelineStep implements PipelineStep {

    static {
        Factories.registerStepRunnerFactory(new Factory());
    }


    private Consumer<Data> consumer;


    public static class Factory implements PipelineStepRunnerFactory {

        @Override
        public boolean canRun(PipelineStep pipelineStep) {
            return pipelineStep instanceof CallbackPipelineStep;
        }

        @Override
        public PipelineStepRunner create(PipelineStep pipelineStep) {
            return new Runner((CallbackPipelineStep)pipelineStep);
        }
    }

    @AllArgsConstructor
    public static class Runner implements PipelineStepRunner {
        private CallbackPipelineStep step;

        @Override
        public void close() {

        }

        @Override
        public PipelineStep getPipelineStep() {
            return step;
        }

        @Override
        public Data exec(Data data) {
            step.consumer.accept(data);
            return data;
        }
    }
}
