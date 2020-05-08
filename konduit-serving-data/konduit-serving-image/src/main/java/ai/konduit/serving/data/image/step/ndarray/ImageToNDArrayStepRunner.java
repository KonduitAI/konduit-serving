package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ImageToNDArrayStepRunner implements PipelineStepRunner {

    protected final ImageToNDArrayStep step;

    public ImageToNDArrayStepRunner(@NonNull ImageToNDArrayStep step){
        this.step = step;
    }

    @Override
    public void close() {
        //No-op
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        /*
        Behaviour:
        (a) If keys are defined, convert only those
        (b) In no keys are defined, convert all images
         */

        List<String> toConvert = step.getKeys();
        if(toConvert == null){
            toConvert = new ArrayList<>();
            for(String s : data.keys()){
                if(data.type(s) == ValueType.IMAGE){
                    toConvert.add(s);
                }
            }
        }

        Data d = Data.empty();
        for(String s : toConvert){
            Image i = data.getImage(s);
            NDArray array = ImageToNDArray.convert(i, step.getConfig());
            d.put(s, array);
        }

        if(step.isKeepOtherValues()) {
            for (String s : data.keys()){
                if(toConvert.contains(s))
                    continue;
                d.copyFrom(s, data);
            }
        }

        return d;
    }
}
