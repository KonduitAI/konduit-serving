package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Builder
@Data
public class ImageToNDArrayStep implements PipelineStep {

    //TODO allow output names to be set

    private ImageToNDArrayConfig config;
    private List<String> keys;
    @Builder.Default
    private boolean keepOtherValues = true;

    public ImageToNDArrayStep(@JsonProperty("config") ImageToNDArrayConfig config, @JsonProperty("keys") List<String> keys,
                              @JsonProperty("keepOtherValues") boolean keepOtherValues){
        this.config = config;
        this.keys = keys;
        this.keepOtherValues = keepOtherValues;
    }

}
