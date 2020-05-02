package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Map;

@Data
public class GraphPipeline implements Pipeline {
    private Map<String, PipelineStep> steps;

    public GraphPipeline(@JsonProperty("steps") Map<String, PipelineStep> steps){
        this.steps = steps;
    }

    @Override
    public PipelineExecutor executor() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
