package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.Im2NDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ImageToNDArrayStep implements PipelineStep {

    //TODO allow output names to be set

    private Im2NDArrayConfig config;
    private List<String> keys;
    @Builder.Default
    private boolean keepOtherValues = true;

}
