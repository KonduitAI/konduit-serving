package ai.konduit.serving.pipeline.generate;

import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.generator.PipelineGenerator;
import ai.konduit.serving.pipeline.generator.impl.PythonPipelineGenerator;

import java.util.ArrayList;
import java.util.List;

public class GeneratePipeline {

    private long seed;
    private int numSteps;


    public List<PipelineStep> getRandomPipelineSteps() {
        List<PipelineStep> ret = new ArrayList<>();
        for(int i = 0; i < numSteps; i++) {
            ret.add(getRandomStep());
        }

        return ret;
    }


    private PipelineStep getRandomStep() {
        PythonPipelineGenerator pythonPipelineGenerator =  PythonPipelineGenerator.builder()
                .inputVariables(null)
                .numNames(1)
                .outputVariables(null)
                .build();

        return pythonPipelineGenerator.generate();
    }

}
