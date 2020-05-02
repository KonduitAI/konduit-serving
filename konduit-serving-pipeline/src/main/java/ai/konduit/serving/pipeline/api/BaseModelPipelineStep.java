package ai.konduit.serving.pipeline.api;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@SuperBuilder
@Data
public abstract class BaseModelPipelineStep<T extends Configuration> implements PipelineStep {

    private String modelUri;
    private T config;

}
