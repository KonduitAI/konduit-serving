package ai.konduit.serving.deeplearning4j.step;

import ai.konduit.serving.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
public class DL4JModelPipelineStep extends BaseModelPipelineStep<DL4JConfiguration> {

    public DL4JModelPipelineStep(String modelUri, DL4JConfiguration config) {
        super(modelUri, config);
    }


}
