package ai.konduit.serving.pipeline.generate;

import ai.konduit.serving.pipeline.PipelineStep;

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

    }

}
