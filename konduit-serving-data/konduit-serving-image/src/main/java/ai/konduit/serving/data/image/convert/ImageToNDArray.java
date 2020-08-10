/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.data.image.convert;

import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.util.ImageUtils;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import com.google.common.primitives.Longs;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.nio.*;

import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

/**
 * Utility method for converting Image objects to NDArrays.
 * See {@link ImageToNDArrayConfig} for more details
 *
 * @author Alex Black
 */
public class ImageToNDArray {

    private ImageToNDArray() {
    }

    /**
     * Convert the provided image to a NDArray, according to the specified configuration<br>
     * See {@link ImageToNDArrayConfig} for more details
     *
     * @param image  Image to convert
     * @param config Configuration to use
     * @return The Image converted to an NDArray
     */
    public static NDArray convert(Image image, ImageToNDArrayConfig config) {
        return convert(image, config, false).getFirst();
    }

    public static Pair<NDArray,BoundingBox> convertWithMetadata(Image image, ImageToNDArrayConfig config) {
        return convert(image, config, true);
    }

    public static BoundingBox getCropRegion(Image image, ImageToNDArrayConfig config){
        int imgH = image.height();
        int imgW = image.width();

        return getCropRegion(imgW, imgH, config);
    }

    public static BoundingBox getCropRegion(int imgW, int imgH, ImageToNDArrayConfig config){
        Integer outH = config.height();
        Integer outW = config.width();
        if (outH == null)
            outH = imgH;
        if (outW == null)
            outW = imgW;


        //Resize if necessary
        boolean correctSize = outH == imgH && outW == imgW;
        if (!correctSize) {
            AspectRatioHandling h = config.aspectRatioHandling();
            if (h == AspectRatioHandling.CENTER_CROP) {
                return centerCropBB(imgH, imgW, outH, outW);
            } else if (h == AspectRatioHandling.PAD) {
                throw new UnsupportedOperationException("Not yet implemented");
            } else if (h == AspectRatioHandling.STRETCH) {
                return BoundingBox.createXY(0.0, 1.0, 0.0, 1.0);
            } else {
                throw new UnsupportedOperationException("Not supported image conversion: " + h);
            }
        } else {
            return BoundingBox.createXY(0.0, 1.0, 0.0, 1.0);
        }
    }


    public static long[] getOutputShape(ImageToNDArrayConfig config){
        int rank = config.includeMinibatchDim() ? 4 : 3;
        long[] out = new long[rank];
        out[0] = 1;
        if(config.format() == NDFormat.CHANNELS_FIRST){
            out[1] = config.channelLayout().numChannels();
            out[2] = config.height();
            out[3] = config.width();
        } else {
            out[1] = config.height();
            out[2] = config.width();
            out[3] = config.channelLayout().numChannels();
        }
        return out;
    }

    protected static Pair<NDArray,BoundingBox> convert(Image image, ImageToNDArrayConfig config, boolean withMeta) {
        BoundingBox bbMeta = null;

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
                Pair<Mat,BoundingBox> p = centerCrop(m, outH, outW, withMeta); //new Mat(m, crop);;
                Mat cropped = p.getFirst();
                if (cropped.cols() == outW && cropped.rows() == outH) {
                    m = cropped;
                } else {
                    Mat resized = new Mat();
                    org.bytedeco.opencv.global.opencv_imgproc.resize(cropped, resized, new Size(outW, outH));
                    m = resized;
                }

                if(withMeta){
                    bbMeta = p.getSecond();
                }
            } else if (h == AspectRatioHandling.PAD) {
                throw new UnsupportedOperationException("Not yet implemented");
            } else if (h == AspectRatioHandling.STRETCH) {
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(outW, outH));
                m = resized;

                if(withMeta){
                    bbMeta = BoundingBox.createXY(0.0, 1.0, 0.0, 1.0);
                }
            } else {
                throw new UnsupportedOperationException("Not supported image conversion: " + h);
            }
        } else {
            if(withMeta){
                bbMeta = BoundingBox.createXY(0.0, 1.0, 0.0, 1.0);
            }
        }

        m = convertColor(m, config);
        Preconditions.checkState(m.channels() <= 3,"Channels must not be greater than 3!");

        ByteBuffer bb = toFloatBuffer(m, config);

        if (config.dataType() != NDArrayType.FLOAT) //TODO there are likely more efficient ways than this!
            bb = ImageUtils.cast(bb, NDArrayType.FLOAT, config.dataType());

        int ch = config.channelLayout().numChannels();

        long[] shape;
        if (config.format() == NDFormat.CHANNELS_FIRST) {
            shape =  new long[]{ch, outH, outW};
        } else {
            shape = new long[]{outH, outW, ch};
        }

        /*   INDArray arrCreate = Nd4j.create(ND4JUtil.typeNDArrayTypeToNd4j(config.dataType()),shape, Nd4j.getStrides(shape),'c');
         *//**
         * Note this logic assumes 3d arrays.
         * Postpone reshape till after the data is filled in
         * since the new shape is just the minibatch size.
         *//*
        ImageUtils.fillNDArray(m,true,arrCreate);
        ByteBuffer bb = arrCreate.data().asNio();*/
        if(config.includeMinibatchDim()) {
            shape = Longs.concat(new long[]{1},shape);
        }

        SerializedNDArray arr = new SerializedNDArray(config.dataType(), shape, bb);

        return new Pair<>(NDArray.create(arr), bbMeta);
    }

    public static Pair<Mat,BoundingBox> centerCrop(Mat image, int outH, int outW, boolean withBB) {
        int imgH = image.rows();
        int imgW = image.cols();

        double aspectIn = image.cols() / (double)image.rows();
        double aspectOut = outW / (double)outH;


        int croppedW;
        int croppedH;
        int x0;
        int x1;
        int y0;
        int y1;
        if(aspectIn == aspectOut){
            //No crop necessary
            return new Pair<>(image, BoundingBox.createXY(0.0, 1.0, 0.0, 1.0));
        } else if(aspectIn > aspectOut){
            //Need to crop from width dimension
            croppedW = (int)(aspectOut * image.rows());
            croppedH = imgH;
            int delta = imgW - croppedW;
            x0 = delta / 2;
            y0 = 0;
        } else {
            //Need to crop from the height dimension
            croppedW = imgW;
            croppedH = (int)(image.cols() / aspectOut);
            int delta = imgH - croppedH;
            x0 = 0;
            y0 = delta / 2;
        }

        Rect crop = new Rect(x0, y0, croppedW, croppedH);
        BoundingBox bb = null;
        if(withBB){
            bb = centerCropBB(imgH, imgW, outH, outW);
        }

        Mat out = image.apply(crop);
        return new Pair<>(out, bb);
    }

    protected static BoundingBox centerCropBB(int imgH, int imgW, int outH, int outW){
        double aspectIn = imgW / (double)imgH;
        double aspectOut = outW / (double)outH;

        int croppedW;
        int croppedH;
        int x0;
        int x1;
        int y0;
        int y1;
        if(aspectIn == aspectOut){
            //No crop necessary
            return BoundingBox.createXY(0.0, 1.0, 0.0, 1.0);
        } else if(aspectIn > aspectOut){
            //Need to crop from width dimension
            croppedW = (int)(aspectOut * imgH);
            croppedH = imgH;
            int delta = imgW - croppedW;
            x0 = delta / 2;
            x1 = imgW - (delta/2);
            y0 = 0;
            y1 = imgH;
        } else {
            //Need to crop from the height dimension
            croppedW = imgW;
            croppedH = (int)(imgW / aspectOut);
            int delta = imgH - croppedH;
            x0 = 0;
            x1 = imgW;
            y0 = delta / 2;
            y1 = imgH - (delta/2);
        }

        double dx1 = x0 / (double)imgW;
        double dx2 = x1 / (double)imgW;
        double dy1 = y0 / (double)imgH;
        double dy2 = y1 / (double)imgH;
        return BoundingBox.createXY(dx1, dx2, dy1, dy2);
    }

    protected static Mat convertColor(Mat m, ImageToNDArrayConfig config) {
        int ch = config.channelLayout().numChannels();
        if (ch != 3) {
            throw new UnsupportedOperationException("Not yet implemented: Channels != 3 support");
        }

        //TODO - Actually convert color!

        return m;
    }

    protected static ByteBuffer toFloatBuffer(Mat m, ImageToNDArrayConfig config) {
        Preconditions.checkState(config.channelLayout() == NDChannelLayout.RGB || config.channelLayout() == NDChannelLayout.BGR,
                "Only RGB and BGR conversion implement so far");

        Preconditions.checkState(config.dataType() != NDArrayType.BOOL && config.dataType() != NDArrayType.UTF8,
                "%s datatype is not supported for ImageToNDArray", config.dataType());

        boolean direct = !Loader.getPlatform().startsWith("android");

        //By default, Mat stores values in channels first format - CHW
        int h = m.rows();
        int w = m.cols();
        int ch = m.channels();

        int lengthElements = h * w * ch;
        int lengthBytes = lengthElements * 4;

        ByteBuffer bb = direct ? ByteBuffer.allocateDirect(lengthBytes).order(ByteOrder.LITTLE_ENDIAN) : ByteBuffer.allocate(lengthBytes).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();

        boolean rgb = config.channelLayout() == NDChannelLayout.RGB;

        ImageUtils.FloatNormalizer f = ImageUtils.getFloatNormalizer(config, rgb);


        Indexer imgIdx = m.createIndexer(direct);
        if (imgIdx instanceof UByteIndexer) {
            UByteIndexer ubIdx = (UByteIndexer) imgIdx;
            if (config.format() == NDFormat.CHANNELS_FIRST) {
                if (rgb) {
                    //Mat is HWC in BGR, we want (N)CHW in RGB format
                    int[] rgbToBgr = {2, 1, 0};
                    for (int c = 0; c < rgbToBgr.length; c++) {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                //int idxBGR = (ch * w * y) + (ch * x) + rgbToBgr[c];
                                int v = ubIdx.get(y,x,rgbToBgr[c]);
                                float normalized = f.normalize(v, c);
                                fb.put(normalized);
                            }
                        }
                    }
                } else {
                    UByteRawIndexer uByteRawIndexer = (UByteRawIndexer) ubIdx;
                    //Mat is HWC in BGR, we want (N)CHW in BGR format
                    for (int c = 0; c < 3; c++) {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int idxBGR = (ch * w * y) + (ch * x) + c;
                                int v = uByteRawIndexer.getRaw(idxBGR);
                                float normalized = f.normalize(v, c);
                                fb.put(normalized);
                            }
                        }
                    }
                }
            } else {
                if (rgb) {
                    //Mat is HWC in BGR, we want (N)HWC in RGB format
                    fb.position(0);
                    UByteRawIndexer uByteRawIndexer = (UByteRawIndexer) ubIdx;
                    for (int i = 0; i < lengthElements; i += 3) {
                        int b = uByteRawIndexer.getRaw(i);
                        int g = uByteRawIndexer.getRaw(i + 1);
                        int r = uByteRawIndexer.getRaw(i + 2);
                        fb.put(f.normalize(r, 0));
                        fb.put(f.normalize(g, 1));
                        fb.put(f.normalize(b, 2));
                    }
                } else {
                    //Mat is HWC in BGR, we want (N)HWC in BGR format
                    UByteRawIndexer uByteRawIndexer = (UByteRawIndexer) ubIdx;

                    for (int i = 0; i < lengthElements; i++) {
                        float normalized = f.normalize(uByteRawIndexer.getRaw(i), i % 3);
                        fb.put(normalized);
                    }
                }
            }
        } else {
            throw new RuntimeException("Not yet implemented: " + imgIdx.getClass());
        }

        return bb;
    }

}
