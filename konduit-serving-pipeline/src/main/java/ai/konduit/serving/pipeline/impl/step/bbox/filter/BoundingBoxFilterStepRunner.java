package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.AllArgsConstructor;

import java.util.List;

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
        List<BoundingBox> boundingBoxes = data.getListBoundingBox("img_bbox");

//       bbox has .label as null. why?
        for (BoundingBox bbox : boundingBoxes
        ) { System.out.println(bbox.label());
//            for (String classToKeep : classesToKeep) {
//                if (!bbox.label().startsWith(classToKeep)) {
//                    boundingBoxes.remove(bbox);
//                }
//            }
        }


        String outName = step.outputName();
        if (outName == null)
            outName = BoundingBoxFilterStep.DEFAULT_OUTPUT_NAME;

        Data d = Data.singletonList(outName, boundingBoxes, ValueType.BOUNDING_BOX);


        return d;
    }


}
