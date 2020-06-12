package ai.konduit.serving.data.image.step.face;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Image;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;

public class CropUtil {

    public static Mat scaleIfRequired(Mat m, DrawFaceKeyPointsStep step){
        if(step.scale() != null && step.scale() != step.scale().NONE){
            boolean scaleRequired = false;
            int newH = 0;
            int newW = 0;
            if(step.scale() == step.scale().AT_LEAST){
                if(m.rows() < step.resizeH() || m.cols() < step.resizeW()){
                    scaleRequired = true;
                    double ar = m.cols() / (double)m.rows();
                    if(m.rows() < step.resizeH() && m.cols() >= step.resizeW()){
                        //Scale height
                        newW = step.resizeW();
                        newH = (int)(newW / ar);
                    } else if(m.rows() > step.resizeH() && m.cols() < step.resizeW()){
                        //Scale width
                        newH = step.resizeH();
                        newW = (int) (ar * newH);
                    } else {
                        //Scale both dims...
                        if((int)(step.resizeW() / ar) < step.resizeH()){
                            //Scale height
                            newW = step.resizeW();
                            newH = (int)(newW / ar);
                        } else {
                            //Scale width
                            newH = step.resizeH();
                            newW = (int) (ar * newH);
                        }
                    }
                }
            } else if(step.scale() == step.scale().AT_MOST){
                Preconditions.checkState(step.resizeH() > 0 && step.resizeW() > 0, "Invalid resize: resizeH=%s, resizeW=%s", step.resizeH(), step.resizeW());
                if(m.rows() > step.resizeH() || m.cols() > step.resizeW()){
                    scaleRequired = true;
                    double ar = m.cols() / (double)m.rows();
                    if(m.rows() > step.resizeH() && m.cols() <= step.resizeW()){
                        //Scale height
                        newW = step.resizeW();
                        newH = (int)(newW / ar);
                    } else if(m.rows() < step.resizeH() && m.cols() > step.resizeW()){
                        //Scale width
                        newH = step.resizeH();
                        newW = (int) (ar * newH);
                    } else {
                        //Scale both dims...
                        if((int)(step.resizeW() / ar) > step.resizeH()){
                            //Scale height
                            newW = step.resizeW();
                            newH = (int)(newW / ar);
                        } else {
                            //Scale width
                            newH = step.resizeH();
                            newW = (int) (ar * newH);
                        }
                    }
                }
            }

            if(scaleRequired){
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(newH, newW));
                return resized;
            } else {
                return m;
            }

        } else {
            return m;
        }
    }


    public static BoundingBox accountForCrop(Image image, BoundingBox bbox, ImageToNDArrayConfig config){
        if(config == null)
            return bbox;

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(image, config);

        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        double x1 = cropRegion.x1() + cropWidth * bbox.x1();
        double x2 = cropRegion.x1() + cropWidth * bbox.x2();
        double y1 = cropRegion.y1() + cropHeight * bbox.y1();
        double y2 = cropRegion.y1() + cropHeight * bbox.y2();
        return BoundingBox.createXY(x1, x2, y1, y2, bbox.label(), bbox.probability());
    }
}
