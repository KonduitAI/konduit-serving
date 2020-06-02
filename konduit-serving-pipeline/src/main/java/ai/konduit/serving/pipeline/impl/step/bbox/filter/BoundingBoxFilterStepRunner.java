package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class BoundingBoxFilterStepRunner implements PipelineStepRunner {

    protected final BoundingBoxFilterStep step;

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }


    @Override
    public Data exec(Context ctx, Data data) {

        String[] classesToKeep = step.classesToKeep();
        List<BoundingBox> boundingBoxes = data
                .getListBoundingBox("img_bbox")
                .stream()
                .filter(i -> !Arrays.stream(classesToKeep).anyMatch(i.label()::equals))
                .collect(Collectors.toList());


        String outName = step.outputName();
        if (outName == null)
            outName = BoundingBoxFilterStep.DEFAULT_OUTPUT_NAME;

        Data d = Data.singletonList(outName, boundingBoxes, ValueType.BOUNDING_BOX);


        return d;
    }


}
