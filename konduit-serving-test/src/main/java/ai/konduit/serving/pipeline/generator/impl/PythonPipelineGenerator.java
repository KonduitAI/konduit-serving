package ai.konduit.serving.pipeline.generator.impl;

import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.generator.PipelineGenerator;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.pipeline.step.PythonStep.PythonStepBuilder;
import ai.konduit.serving.util.python.PythonVariables;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class PythonPipelineGenerator implements PipelineGenerator {
    @Default
    private int numNames = 1;

    private PythonVariables inputVariables,outputVariables;

    @Override
    public PipelineStep generate() {
        PythonStepBuilder<?, ?> builder = PythonStep.builder();
        //generate random python configuration
        for(int i = 0; i < numNames; i++)
            builder.pythonConfig(UUID.randomUUID().toString(),getConfig());

        return builder.build();
    }

    private PythonConfig getConfig() {
        /**
         * Need to figure out how we want to generate random input variables
         * and code
         */
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonCode("")
                .build();

        return pythonConfig;
    }

}
