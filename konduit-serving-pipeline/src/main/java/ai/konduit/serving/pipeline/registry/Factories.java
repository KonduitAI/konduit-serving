package ai.konduit.serving.pipeline.registry;

import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

@Slf4j
public class Factories {

    private static List<PipelineStepRunnerFactory> stepRunnerFactories;

    public static List<PipelineStepRunnerFactory> getStepRunnerFactories(){
        if(stepRunnerFactories == null)
            initStepRunnerFactories();

        return stepRunnerFactories;
    }

    private static void initStepRunnerFactories(){

        ServiceLoader<PipelineStepRunnerFactory> sl = ServiceLoader.load(PipelineStepRunnerFactory.class);
        Iterator<PipelineStepRunnerFactory> iterator = sl.iterator();
        List<PipelineStepRunnerFactory> f = new ArrayList<>();
        while(iterator.hasNext()){
            f.add(iterator.next());
        }

        stepRunnerFactories = f;
        log.info("Loaded {} PipelineStepRunnerFactory instances", f.size());
    }

    public static void registerStepRunnerFactory(@NonNull PipelineStepRunnerFactory f){
        if(stepRunnerFactories == null)
            initStepRunnerFactories();
        stepRunnerFactories.add(f);
    }

}
