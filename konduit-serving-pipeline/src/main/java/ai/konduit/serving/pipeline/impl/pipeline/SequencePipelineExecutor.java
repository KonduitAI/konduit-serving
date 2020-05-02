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
import org.nd4j.common.base.Preconditions;
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

            if(f == null){
                StringBuilder msg = new StringBuilder("Unable to execute pipeline step of type " + ps.getClass().getName() + ": No PipelineStepRunnerFactory instances"
                        + " are available that can execute this pipeline step.\nThis likely means a required dependency is missing for executing this pipeline." +
                        "\nAvailable executor factories:");
                if(factories.isEmpty()){
                    msg.append(" <None>");
                }
                for(PipelineStepRunnerFactory psrf : factories){
                    msg.append("  ").append(psrf.getClass().getName());
                }
                throw new IllegalStateException(msg.toString());
            }

            PipelineStepRunner r = f.create(ps);
            Preconditions.checkNotNull(r, "Failed to create PipelineStepRunner: PipelineStepRunnerFactory.create(...) returned null: " +
                    "Pipeline step %s, PipelineStepRunnerFactory %s", ps.getClass(), f.getClass());
            runners.add(r);
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
