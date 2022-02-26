/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.data.image.util;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.ImageNormalization;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.data.Point;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.util.ArrayUtil;
import org.nd4j.linalg.api.concurrency.AffinityManager;
import org.nd4j.linalg.api.memory.pointers.PagedPointer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.nativeblas.NativeOpsHolder;

import java.nio.*;
import java.util.function.IntToDoubleFunction;

public class ImageUtils {

    private ImageUtils(){ }


    /**
     * Adapted from datavec's NativeImageLoader
     * @param image the input image
     * @param direct whether the image is direct or not
     * @param ret the input ndarray
     */
    public static  void fillNDArray(Mat image, boolean direct,INDArray ret) {
        long rows = image.rows();
        long cols = image.cols();
        long channels = image.channels();

        if (ret.length() != rows * cols * channels) {
            throw new ND4JIllegalStateException("INDArray provided to store image not equal to image: {channels: "
                    + channels + ", rows: " + rows + ", columns: " + cols + "}");
        }

        Indexer idx = image.createIndexer(direct);
        Pointer pointer = ret.data().pointer();
        long[] stride = ret.stride();
        boolean done = false;
        PagedPointer pagedPointer = new PagedPointer(pointer, rows * cols * channels,
                ret.data().offset() * Nd4j.sizeOfDataType(ret.data().dataType()));

        if (pointer instanceof FloatPointer) {
            FloatIndexer retidx = FloatIndexer.create(pagedPointer.asFloatPointer(),
                    new long[] {channels, rows, cols}, new long[] {stride[0], stride[1], stride[2]}, direct);
            if (idx instanceof UByteIndexer) {
                UByteIndexer ubyteidx = (UByteIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, ubyteidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof UShortIndexer) {
                UShortIndexer ushortidx = (UShortIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, ushortidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof IntIndexer) {
                IntIndexer intidx = (IntIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, intidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof FloatIndexer) {
                FloatIndexer floatidx = (FloatIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, floatidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            }
            retidx.release();
        } else if (pointer instanceof DoublePointer) {
            DoubleIndexer retidx = DoubleIndexer.create(pagedPointer.asDoublePointer(),
                    new long[] {channels, rows, cols}, new long[] {stride[0], stride[1], stride[2]}, direct);
            if (idx instanceof UByteIndexer) {
                UByteIndexer ubyteidx = (UByteIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, ubyteidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof UShortIndexer) {
                UShortIndexer ushortidx = (UShortIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, ushortidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof IntIndexer) {
                IntIndexer intidx = (IntIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, intidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            } else if (idx instanceof FloatIndexer) {
                FloatIndexer floatidx = (FloatIndexer) idx;
                for (long k = 0; k < channels; k++) {
                    for (long i = 0; i < rows; i++) {
                        for (long j = 0; j < cols; j++) {
                            retidx.put(k, i, j, floatidx.get(i, j, k));
                        }
                    }
                }
                done = true;
            }
            retidx.release();
        }


        if (!done) {
            for (long k = 0; k < channels; k++) {
                for (long i = 0; i < rows; i++) {
                    for (long j = 0; j < cols; j++) {
                        if (ret.rank() == 3) {
                            ret.putScalar(k, i, j, idx.getDouble(i, j, k));
                        } else if (ret.rank() == 4) {
                            ret.putScalar(1, k, i, j, idx.getDouble(i, j, k));
                        } else if (ret.rank() == 2) {
                            ret.putScalar(i, j, idx.getDouble(i, j));
                        } else
                            throw new ND4JIllegalStateException("NativeImageLoader expects 2D, 3D or 4D output array, but " + ret.rank() + "D array was given");
                    }
                }
            }
        }

        idx.release();
        image.data();
        Nd4j.getAffinityManager().tagLocation(ret, AffinityManager.Location.HOST);
    }


    public static BoundingBox accountForCrop(Image image, BoundingBox bbox, ImageToNDArrayConfig config) {
        return accountForCrop(bbox, image.width(), image.height(), config);
    }

    public static BoundingBox accountForCrop(BoundingBox bbox, int width, int height, ImageToNDArrayConfig config) {
        if (config == null)
            return bbox;

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(width, height, config);

        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        double x1 = cropRegion.x1() + cropWidth * bbox.x1();
        double x2 = cropRegion.x1() + cropWidth * bbox.x2();
        double y1 = cropRegion.y1() + cropHeight * bbox.y1();
        double y2 = cropRegion.y1() + cropHeight * bbox.y2();

        return BoundingBox.createXY(x1, x2, y1, y2, bbox.label(), bbox.probability());
    }


    public static Point accountForCrop(Point relPoint, int width, int height, ImageToNDArrayConfig imageToNDArrayConfig) {
        if(imageToNDArrayConfig == null){
            return relPoint.toAbsolute(width, height);
        }

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(width, height, imageToNDArrayConfig);
        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        return Point.create(
                cropRegion.x1() + cropWidth * relPoint.x(),
                cropRegion.y1() + cropHeight * relPoint.y(),
                relPoint.label(),
                relPoint.probability()
        ).toAbsolute(width, height);
    }

    /**
     * Get a float normalizer based on the input
     * {@link ImageToNDArrayConfig}
     * @param config the input configuration
     * @param rgb whether the image layout is rgb or bgr
     * @return the {@link FloatNormalizer} based on the
     * given configuration
     */
    public static FloatNormalizer getFloatNormalizer(ImageToNDArrayConfig config, boolean rgb) {
        FloatNormalizer f;
        ImageNormalization n = config.normalization();
        if(n == null || n.type() == ImageNormalization.Type.NONE) {
            f = (x,c) -> x;     //No-op
        } else {
            switch (config.normalization().type()) {
                case SCALE:
                    float scale = (n.maxValue() == null ? 255.0f : n.maxValue().floatValue()) / 2.0f;
                    f = (x,c) -> (x / scale - 1.0f);
                    break;
                case SCALE_01:
                    float scale01 = n.maxValue() == null ? 255.0f : n.maxValue().floatValue();
                    f = (x,c) -> (x / scale01);
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
                case IMAGE_NET:
                    double[] imagenetMeanRgb = ImageNormalization.getImagenetMeanRgb();
                    double[] imageNetMeanStd = ImageNormalization.getImageNetStdRgb();
                    float[] imageNetNormalized = rgb ? ArrayUtil.toFloats(imagenetMeanRgb) : new float[]{(float) imagenetMeanRgb[2], (float) imagenetMeanRgb[1], (float) imagenetMeanRgb[0]};
                    float[] imageNetStdNormalized = rgb ? ArrayUtil.toFloats(imageNetMeanStd) : new float[]{(float) imageNetMeanStd[2], (float) imageNetMeanStd[1], (float) imageNetMeanStd[0]};
                    f = (x,c) -> x - imageNetNormalized[c] / imageNetStdNormalized[c];
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported image normalization type: " + config.normalization().type());
            }
        }
        return f;
    }

    /**
     * Cast the given {@link ByteBuffer}
     * from the fromTYpe to the toType
     * @param from the input bytebuffer to cast
     * @param fromType the from type
     * @param toType the new type of the bytebuffer
     * @return the output bytebuffer
     */
    public static ByteBuffer cast(ByteBuffer from, NDArrayType fromType, NDArrayType toType) {
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
        ByteBuffer bb = direct ? ByteBuffer.allocateDirect(bytesLength).order(ByteOrder.LITTLE_ENDIAN) : ByteBuffer.allocate(bytesLength).order(ByteOrder.LITTLE_ENDIAN);

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
                ShortBuffer sb = bb.asShortBuffer();
                for (int i = 0; i < length; i++)
                    sb.put((short) f.applyAsDouble(i));
                break;
            case INT8:
                for (int i = 0; i < length; i++)
                    bb.put((byte) f.applyAsDouble(i));
                break;
            case UINT8:
                //TODO inefficient - x -> double -> int -> uint8
                UByteIndexer idx_ui8 = UByteIndexer.create(bb);
                for( int i=0; i<length; i++ )
                    idx_ui8.put(i, (int)f.applyAsDouble(i));
                break;
            case FLOAT16:
                HalfIndexer idx_f16 = HalfIndexer.create(bb.asShortBuffer());
                for( int i=0; i<length; i++)
                    idx_f16.put(i, (float)f.applyAsDouble(i));
                break;
            case BFLOAT16:
                Bfloat16Indexer idx_bf16 = Bfloat16Indexer.create(bb.asShortBuffer());
                for( int i=0; i<length; i++ )
                    idx_bf16.put(i, (float)f.applyAsDouble(i));
                break;
            case UINT64:
                ULongIndexer idx_ui64 = ULongIndexer.create(bb.asLongBuffer());
                for( int i=0; i<length; i++)
                    idx_ui64.put(i, (long)f.applyAsDouble(i));
                break;
            case UINT32:
                UIntIndexer idx_ui32 = UIntIndexer.create(bb.asIntBuffer());
                for( int i=0; i<length; i++ )
                    idx_ui32.put(i, (int)f.applyAsDouble(i));
                break;
            case UINT16:
                UShortIndexer idx_ui16 = UShortIndexer.create(bb.asShortBuffer());
                for( int i=0; i<length; i++ )
                    idx_ui16.put(i, (int)f.applyAsDouble(i));
                break;
            default:
                throw new UnsupportedOperationException("Conversion to " + fromType + " to " + toType + " not supported or not yet implemented");
        }
        return bb;
    }


    public interface FloatNormalizer {
        float normalize(float f, int channel);
    }
}
