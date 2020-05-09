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
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
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
import org.nd4j.common.util.ArrayUtil;

import java.nio.*;
import java.util.function.IntToDoubleFunction;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

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

        if (config.dataType() != NDArrayType.FLOAT) //TODO there are likely more efficient ways than this!
            bb = cast(bb, NDArrayType.FLOAT, config.dataType());

        int ch = config.channelLayout().numChannels();

        long[] shape;
        if (config.format() == NDFormat.CHANNELS_FIRST) {
            shape = config.includeMinibatchDim() ? new long[]{1, ch, outH, outW} : new long[]{ch, outH, outW};
        } else {
            shape = config.includeMinibatchDim() ? new long[]{1, outH, outW, ch} : new long[]{outH, outW, ch};
        }

        SerializedNDArray arr = new SerializedNDArray(config.dataType(), shape, bb);

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

    protected static Mat convertColor(Mat m, ImageToNDArrayConfig config) {
        int ch = config.channelLayout().numChannels();
        if (ch != 3) {
            throw new UnsupportedOperationException("Not yet implemented: Channels != 3 support");
        }

        //TODO - Actually convert color!

        return m;
    }

    public interface FloatNormalizer {
        float normalize(float f, int channel);
    }

    protected static ByteBuffer toFloatBuffer(Mat m, ImageToNDArrayConfig config) {
        Preconditions.checkState(config.channelLayout() == NDChannelLayout.RGB || config.channelLayout() == NDChannelLayout.BGR,
                "Only RGB and BGR conversion implement so far");

        boolean direct = !Loader.getPlatform().startsWith("android");

        //By default, Mat stores values in channels first format - CHW
        int h = m.rows();
        int w = m.cols();
        int ch = m.channels();

        int lengthElements = h * w * ch;
        int lengthBytes = lengthElements * 4;

        ByteBuffer bb = direct ? ByteBuffer.allocateDirect(lengthBytes) : ByteBuffer.allocate(lengthBytes);
        FloatBuffer fb = bb.asFloatBuffer();

        boolean rgb = config.channelLayout() == NDChannelLayout.RGB;

        FloatNormalizer f;
        ImageNormalization n = config.normalization();
        if(n == null || n.type() == ImageNormalization.Type.NONE){
            f = (x,c) -> x;     //No-op
        } else {
            switch (config.normalization().type()){
                case SCALE:
                    float scale = n.maxValue() == null ? 255.0f : n.maxValue().floatValue();
                    f = (x,c) -> (x / scale);
                    break;
                case SUBTRACT_MEAN:
                    //TODO support grayscale
                    Preconditions.checkState(n.meanRgb() != null, "Error during normalization: Normalization type is set to " +
                            "SUBTRACT_MEAN but not meanRgb array is provided");
                    double[] mrgb = n.meanRgb();
                    float[] channelMeans = rgb ? ArrayUtil.toFloats(mrgb) : new float[]{(float) mrgb[2], (float) mrgb[1], (float) mrgb[0]};
                    f = (x,c) -> (x - channelMeans[c]);
                    break;
                case STANDARDIZE:
                    Preconditions.checkState(n.meanRgb() != null, "Error during normalization: Normalization type is set to " +
                            "STANDARDIZE but not meanRgb array is provided");
                    Preconditions.checkState(n.stdRgb() != null, "Error during normalization: Normalization type is set to " +
                            "STANDARDIZE but not stdRgb array is provided");
                    double[] mrgb2 = n.meanRgb();
                    double[] stdrgb = n.stdRgb();
                    float[] channelMeans2 = rgb ? ArrayUtil.toFloats(mrgb2) : new float[]{(float) mrgb2[2], (float) mrgb2[1], (float) mrgb2[0]};
                    float[] channelStd = rgb ? ArrayUtil.toFloats(stdrgb) : new float[]{(float) stdrgb[2], (float) stdrgb[1], (float) stdrgb[0]};
                    f = (x,c) -> ( (x-channelMeans2[c]) / channelStd[c]);
                    break;
                case INCEPTION:
                    float scale2 = n.maxValue() == null ? 255.0f : n.maxValue().floatValue();
                    f = (x,c) -> ( ((x/scale2) - 0.5f) * 2.0f );
                    break;
                case VGG_SUBTRACT_MEAN:
                    double[] mrgbVgg = ImageNormalization.getVggMeanRgb();
                    float[] channelMeansVGG = rgb ? ArrayUtil.toFloats(mrgbVgg) : new float[]{(float) mrgbVgg[2], (float) mrgbVgg[1], (float) mrgbVgg[0]};
                    f = (x,c) -> (x - channelMeansVGG[c]);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported image normalization type: " + config.normalization().type());
            }
        }



        Indexer imgIdx = m.createIndexer(direct);
        if (imgIdx instanceof UByteIndexer) {
            UByteIndexer ubIdx = (UByteIndexer) imgIdx;

            if (config.format() == NDFormat.CHANNELS_FIRST) {
                if (rgb) {
                    //Mat is HWC in BGR, we want (N)CHW in RGB format
                    int[] rgbToBgr = {2, 1, 0};
                    for (int c = 0; c < 3; c++) {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int idxBGR = (ch * w * y) + (ch * x) + rgbToBgr[c];
                                int v = ubIdx.get(idxBGR);
                                float normalized = f.normalize(v, c);
                                fb.put(normalized);
                            }
                        }
                    }
                } else {
                    //Mat is HWC in BGR, we want (N)CHW in BGR format
                    for (int c = 0; c < 3; c++) {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int idxBGR = (ch * w * y) + (ch * x) + c;
                                int v = ubIdx.get(idxBGR);
                                float normalized = f.normalize(v, c);
                                fb.put(normalized);
                            }
                        }
                    }
                }
            } else {
                if (rgb) {
                    //Mat is HWC in BGR, we want (N)HWC in RGB format
                    for (int i = 0; i < lengthElements; i += 3) {
                        int b = ubIdx.get(i);
                        int g = ubIdx.get(i + 1);
                        int r = ubIdx.get(i + 2);
                        fb.put(f.normalize(r, 0));
                        fb.put(f.normalize(g, 1));
                        fb.put(f.normalize(b, 2));
                    }
                } else {
                    //Mat is HWC in BGR, we want (N)HWC in BGR format
                    for (int i = 0; i < lengthElements; i++) {
                        float normalized = f.normalize(ubIdx.get(i), i % 3);
                        fb.put(normalized);
                    }
                }
            }
        } else {
            throw new RuntimeException("Not yet implemented: " + imgIdx.getClass());
        }

        return bb;
    }

    //TODO This isn't the most efficient or elegant approach, but it should work OK for images
    protected static ByteBuffer cast(ByteBuffer from, NDArrayType fromType, NDArrayType toType) {
        if (fromType == toType)
            return from;

        boolean direct = !Loader.getPlatform().startsWith("android");


        IntToDoubleFunction f;

        int length;
        switch (fromType) {
            case DOUBLE:
                DoubleBuffer db = from.asDoubleBuffer();
                length = db.limit();
                f = db::get;
                break;
            case FLOAT:
                FloatBuffer fb = from.asFloatBuffer();
                length = fb.limit();
                f = fb::get;
                break;
            case INT64:
                LongBuffer lb = from.asLongBuffer();
                length = lb.limit();
                f = i -> (double) lb.get();
                break;
            case INT32:
                IntBuffer ib = from.asIntBuffer();
                length = ib.limit();
                f = ib::get;
                break;
            case INT16:
                ShortBuffer sb = from.asShortBuffer();
                length = sb.limit();
                f = sb::get;
                break;
            case INT8:
                length = from.limit();
                f = from::get;
                break;
            case FLOAT16:
            case BFLOAT16:
            case UINT64:
            case UINT32:
            case UINT16:
            case UINT8:
            case BOOL:
            case UTF8:
            default:
                throw new UnsupportedOperationException("Conversion to " + fromType + " not supported or not yet implemented");
        }

        int bytesLength = toType.width() * length;
        ByteBuffer bb = direct ? ByteBuffer.allocateDirect(bytesLength) : ByteBuffer.allocate(bytesLength);

        switch (toType) {
            case DOUBLE:
                DoubleBuffer db = bb.asDoubleBuffer();
                for (int i = 0; i < length; i++)
                    db.put(f.applyAsDouble(i));
                break;
            case FLOAT:
                FloatBuffer fb = bb.asFloatBuffer();
                for (int i = 0; i < length; i++)
                    fb.put((float) f.applyAsDouble(i));
                break;
            case INT64:
                LongBuffer lb = bb.asLongBuffer();
                for (int i = 0; i < length; i++)
                    lb.put((long) f.applyAsDouble(i));
                break;
            case INT32:
                IntBuffer ib = bb.asIntBuffer();
                for (int i = 0; i < length; i++)
                    ib.put((int) f.applyAsDouble(i));
                break;
            case INT16:
                ShortBuffer sb = from.asShortBuffer();
                for (int i = 0; i < length; i++)
                    sb.put((short) f.applyAsDouble(i));
                break;
            case INT8:
                for (int i = 0; i < length; i++)
                    bb.put((byte) f.applyAsDouble(i));
                break;
            case FLOAT16:
            case BFLOAT16:
            case UINT64:
            case UINT32:
            case UINT16:
            case UINT8:
            case BOOL:
            case UTF8:
            default:
                throw new UnsupportedOperationException("Conversion to " + fromType + " to " + toType + " not supported or not yet implemented");
        }
        return bb;
    }
}
