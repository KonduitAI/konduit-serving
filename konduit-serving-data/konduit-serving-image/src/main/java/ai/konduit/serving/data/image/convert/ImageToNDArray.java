package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;

public class ImageToNDArray {

    private ImageToNDArray(){ }

    public static NDArray convert(Image image, ImageConvertConfig config){

        Integer outH = config.height();
        Integer outW = config.width();
        if(outH == null)
            outH = image.height();
        if(outW == null)
            outW = image.width();

        boolean correctSize = outH == image.height() && outW == image.width();
        Mat m = image.getAs(Mat.class);
        if(!correctSize){
            AspectRatioHandling h = config.aspectRatioHandling();
            if(h == AspectRatioHandling.CENTER_CROP){

            } else if(h == AspectRatioHandling.PAD){

            } else if(h == AspectRatioHandling.STRETCH){

            } else {
                throw new UnsupportedOperationException("Not supported image conversion: " + h);
            }
        }



        boolean direct = !Loader.getPlatform().startsWith("android");


        throw new UnsupportedOperationException();
    }

    private ByteBuffer toFloat(Mat m)

}
