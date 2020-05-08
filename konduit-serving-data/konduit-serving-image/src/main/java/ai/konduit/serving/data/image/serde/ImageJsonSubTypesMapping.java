package ai.konduit.serving.data.image.serde;

import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.data.image.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.util.ArrayList;
import java.util.List;

public class ImageJsonSubTypesMapping implements JsonSubTypesMapping {
    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("SHOW_IMAGE", ShowImagePipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("IMAGE_TO_NDARRAY", ImageToNDArrayStep.class, PipelineStep.class));
        return l;
    }
}
