package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class BoundingBoxFilterStep implements PipelineStep {

    public static final String DEFAULT_OUTPUT_NAME = "bounding_boxes";


    @Builder.Default
    protected String[] classesToKeep = new String[]{};

    @Builder.Default
    protected String inputName = "input";

    @Builder.Default
    protected String outputName = DEFAULT_OUTPUT_NAME;

    public BoundingBoxFilterStep(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.classesToKeep = new String[]{};
        this.outputName = DEFAULT_OUTPUT_NAME;
        this.inputName = "input";
    }
}
