package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;

public class ImageToNDArray {

    private ImageToNDArray(){ }

    public static NDArray convert(Image image, ImageConvertConfig config){

        Integer outH = config.height();
        Integer outW = config.width();
        if(outH == null)
            outH = image.height();
        if(outW == null)
            outW = image.width();



        throw new UnsupportedOperationException();
    }

}
