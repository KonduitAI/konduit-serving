package ai.konduit.serving.camera.serde;

import ai.konduit.serving.camera.step.capture.FrameCapturePipelineStep;
import ai.konduit.serving.camera.step.show.ShowImagePipelineStep;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.util.ArrayList;
import java.util.List;

public class CameraJsonSubTypesMapping implements JsonSubTypesMapping {
    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("FRAME_CAPTURE", FrameCapturePipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("SHOW_IMAGE", ShowImagePipelineStep.class, PipelineStep.class));
        return l;
    }
}
