package ai.konduit.serving.pipeline.impl.serde;

import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;

import java.util.ArrayList;
import java.util.List;

public class PipelineCoreSubtypesMapping implements JsonSubTypesMapping {


    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("LOGGING", LoggingPipelineStep.class, PipelineStep.class));

        return l;
    }
}
