package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.registry.Factories;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SequencePipelineExecutor implements PipelineExecutor {

    private SequencePipeline pipeline;
    private List<PipelineStepRunner> runners;

    public SequencePipelineExecutor(@NonNull SequencePipeline p){
        this.pipeline = p;

        //Initialize
        runners = new ArrayList<>();
        List<PipelineStep> steps = p.getSteps();

        List<PipelineStepRunnerFactory> factories = Factories.getStepRunnerFactories();

        for(PipelineStep ps : steps){
            PipelineStepRunnerFactory f = null;
            for(PipelineStepRunnerFactory psrf : factories){
                if(psrf.canRun(ps)){
                    if(f != null){
                        log.warn("TODO - Multiple PipelineStepRunnerFactory instances can run pipeline {} - {} and {}", ps.getClass(), f.getClass(), psrf.getClass());
                    }

                    f = psrf;
                    //TODO make debug level later
                    log.info("PipelineStepRunnerFactory {} used to run step {}", psrf.getClass().getName(), ps.getClass().getName());
                }
            }
            runners.add(f.create(ps));
        }
    }


    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public List<PipelineStepRunner> getRunners() {
        return runners;
    }

    @Override
    public Data exec(Data data) {
        Data current = data;
        for(PipelineStepRunner psr : runners){
            current = psr.exec(current);
        }
        return current;
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}
