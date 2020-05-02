package ai.konduit.serving.deeplearning4j.serde;

import ai.konduit.serving.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.Configuration;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.util.ArrayList;
import java.util.List;

public class DL4JJsonSubTypesMapping implements JsonSubTypesMapping {


    @Override
    public List<JsonSubType> getSubTypesMapping() {

        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("DEEPLEARNING4J", DL4JModelPipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("DEEPLEARNING4J_CONFIG", DL4JConfiguration.class, Configuration.class));

        return l;
    }
}
