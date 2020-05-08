package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.NDChannels;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class ImageToNDArray {

    private ImageToNDArray() {
    }

    public static NDArray convert(Image image, Im2NDArrayConfig config) {

        Integer outH = config.height();
        Integer outW = config.width();
        if (outH == null)
            outH = image.height();
        if (outW == null)
            outW = image.width();


        //Resize if necessary
        boolean correctSize = outH == image.height() && outW == image.width();
        Mat m = image.getAs(Mat.class);
        if (!correctSize) {
            AspectRatioHandling h = config.aspectRatioHandling();
            if (h == AspectRatioHandling.CENTER_CROP) {
                Mat cropped = centerCrop(m); //new Mat(m, crop);
                if (cropped.cols() == outW && cropped.rows() == outH) {
                    m = cropped;
                } else {
                    Mat resized = new Mat();
                    org.bytedeco.opencv.global.opencv_imgproc.resize(cropped, resized, new Size(outW, outH));
                    m = resized;
                }
            } else if (h == AspectRatioHandling.PAD) {
                throw new UnsupportedOperationException("Not yet implemented");
            } else if (h == AspectRatioHandling.STRETCH) {
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(outW, outH));
                m = resized;
            } else {
                throw new UnsupportedOperationException("Not supported image conversion: " + h);
            }
        }

        m = convertColor(m, config);

        ByteBuffer bb = toFloatBuffer(m, config);

//        float[] temp = new float[100];
//        bb.asFloatBuffer().get(temp);
//        System.out.println(Arrays.toString(temp));

        int ch = config.channels().numChannels();

        long[] shape;
        if (config.format() == NDFormat.CHANNELS_FIRST) {
            shape = config.includeMinibatchDim() ? new long[]{1, ch, outH, outW} : new long[]{ch, outH, outW};
        } else {
            shape = config.includeMinibatchDim() ? new long[]{1, outH, outW, ch} : new long[]{outH, outW, ch};
        }

        SerializedNDArray arr = new SerializedNDArray(NDArrayType.FLOAT, shape, bb);

        return NDArray.create(arr);
    }

    protected static Mat centerCrop(Mat image) {
        int imgH = image.rows();
        int imgW = image.cols();

        int x = 0;
        int y = 0;
        int newHW;
        int cropSize = Math.abs(imgH - imgW) / 2;
        if (imgH > imgW) {
            newHW = imgW;
            y = cropSize;
        } else {
            x = cropSize;
            newHW = imgH;
        }
        Rect crop = new Rect(x, y, newHW, newHW);
        return image.apply(crop);
    }

    protected static Mat convertColor(Mat m, Im2NDArrayConfig config) {
        int ch = config.channels().numChannels();
        if (ch != 3) {
            throw new UnsupportedOperationException("Not yet implemented: Channels != 3 support");
        }

        //TODO

        return m;
    }

    protected static ByteBuffer toFloatBuffer(Mat m, Im2NDArrayConfig config) {
        Preconditions.checkState(config.channels() == NDChannels.RGB || config.channels() == NDChannels.BGR,
                "Only RGB and BGR conversion implement so far");


        boolean direct = !Loader.getPlatform().startsWith("android");
        Indexer mIdx = m.createIndexer(direct);

        //By default, Mat stores values in channels first format - CHW
        int h = m.rows();
        int w = m.cols();
        int ch = m.channels();

        int lengthElements = h * w * ch;
        int lengthBytes = lengthElements * 4;

        ByteBuffer bb = direct ? ByteBuffer.allocateDirect(lengthBytes) : ByteBuffer.allocate(lengthBytes);
        FloatBuffer fb = bb.asFloatBuffer();

        boolean rgb = config.channels() == NDChannels.RGB;

        Indexer imgIdx = m.createIndexer(direct);
        if (imgIdx instanceof UByteIndexer) {
            UByteIndexer ubIdx = (UByteIndexer) imgIdx;

            if (config.format() == NDFormat.CHANNELS_FIRST) {
                if(rgb){
                    //Mat is HWC in BGR, we want (N)CHW in RGB format
                    int[] rgbToBgr = {2,1,0};
                    for( int c = 0; c<3; c++ ){
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int idxBGR = (ch * w * y) + (ch * x) + rgbToBgr[c];
                                int v = ubIdx.get(idxBGR);
                                fb.put(v);
                            }
                        }
                    }
                } else {
                    //Mat is HWC in BGR, we want (N)CHW in BGR format
                    for( int c = 0; c<3; c++ ){
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int idxBGR = (ch * w * y) + (ch * x) + c;
                                int v = ubIdx.get(idxBGR);
                                fb.put(v);
                            }
                        }
                    }
                }
            } else {
                if (rgb) {
                    //Mat is HWC in BGR, we want (N)HWC in RGB format
                    for (int i = 0; i < lengthElements; i += 3) {
                        int b = ubIdx.get(i);
                        int g = ubIdx.get(i+1);
                        int r = ubIdx.get(i+2);
                        fb.put(r);
                        fb.put(g);
                        fb.put(b);
                    }
                } else {
                    //Mat is HWC in BGR, we want (N)HWC in BGR format
                    for (int i = 0; i < lengthElements; i++) {
                        fb.put(ubIdx.get(i));
                    }
                }
            }
        } else {
            throw new RuntimeException("Not yet implemented: " + imgIdx.getClass());
        }


        return bb;
    }

}
