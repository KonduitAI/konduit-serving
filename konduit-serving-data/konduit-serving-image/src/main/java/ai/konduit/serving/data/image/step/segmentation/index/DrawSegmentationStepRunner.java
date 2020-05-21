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

package ai.konduit.serving.data.image.step.segmentation.index;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.NonNull;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;
import org.opencv.core.CvType;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DrawSegmentationStepRunner implements PipelineStepRunner {

    protected final DrawSegmentationStep step;
    protected int[] colorsB;
    protected int[] colorsG;
    protected int[] colorsR;

    public DrawSegmentationStepRunner(@NonNull DrawSegmentationStep step) {
        this.step = step;
    }

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        if(colorsB == null) {
            List<String> classColors = step.classColors();
            initColors(classColors, 32);
        }

        NDArray segmentArr = data.getNDArray(step.segmentArray());
        long[] shape = segmentArr.shape();
        Preconditions.checkState(shape.length == 3 && shape[0] == 1, "Expected segment indices array with shape [1, height, width]," +
                " got array with shape %s", shape);

        boolean drawingOnImage;
        Mat drawOn;
        String imgName = step.image();

        boolean resizeRequired = false;
        if (imgName == null) {
            drawOn = new Mat((int) shape[1], (int) shape[2], CvType.CV_8UC3);       //8 bits per chanel RGB
            drawingOnImage = false;
        } else {
            Image i = data.getImage(imgName);
            int iH = i.height();
            int iW = i.width();

            double arImg = iW / (double)iH;
            double arSegment = shape[2] / (double)shape[1];

            if(iH != shape[1] && iW != shape[2]) {
                if(arImg == arSegment){
                    //OK
                    resizeRequired = true;
                } else {
                    resizeRequired = true;
//                    wasCropped = true;
                    Preconditions.checkState(step.imageToNDArrayConfig() != null, "Image and segment indices array dimensions do not match in terms" +
                            " of aspect ratio, and no ImageToNDArrayConfig was provided. Expected segment indices array with shape [1, height, width] - got array with shape %s and image with h=%s, w=%s", shape, iH, iW);
                }
                drawOn = new Mat((int) shape[1], (int) shape[2], CvType.CV_8UC3);       //8 bits per chanel RGB
                drawingOnImage = false;
            } else {
                drawOn = new Mat();
                i.getAs(Mat.class).clone().convertTo(drawOn, CvType.CV_8UC3);
                drawingOnImage = true;
            }
        }

        SerializedNDArray nd = segmentArr.getAs(SerializedNDArray.class);
        long[] maskShape = nd.getShape();
        int h = (int) maskShape[1];
        int w = (int) maskShape[2];

        //TODO ideally we'd use OpenCV's bitwise methods to do this, but it seems like JavaCV doesn't have those...
        UByteIndexer idx = drawOn.createIndexer();      //HWC BGR format


        IntGetter ig = null;
        if (nd.getType() == NDArrayType.INT32) {
            IntBuffer ib = nd.getBuffer().asIntBuffer();
            ig = ib::get;
        } else if (nd.getType() == NDArrayType.INT64) {
            nd.getBuffer().rewind();
            LongBuffer lb = nd.getBuffer().asLongBuffer();
            ig = () -> (int)lb.get();
        } else {
            throw new RuntimeException();
        }

        boolean skipBackgroundClass = step.backgroundClass() != null;
        int backgroundClass = skipBackgroundClass ? step.backgroundClass() : -1;

        if(drawingOnImage){
            double opacity;
            if(step.opacity() == null) {
                opacity = DrawSegmentationStep.DEFAULT_OPACITY;
            } else {
                opacity = step.opacity();
                Preconditions.checkState(opacity >= 0.0 && opacity <= 1.0, "Opacity value (if set) must be between 0.0 and 1.0, got %s", opacity);
            }
            double o2 = 1.0 - opacity;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int classIdx = ig.get();
                    if(classIdx >= colorsB.length)
                        initColors(step.classColors(), colorsB.length + 32);

                    long idxB = (3 * w * y) + (3 * x);

                    int b,g,r;
                    if(skipBackgroundClass && classIdx == backgroundClass){
                        b = idx.get(idxB);
                        g = idx.get(idxB+1);
                        r = idx.get(idxB+2);
                    } else {
                        b = (int) (opacity * colorsB[classIdx] + o2 * idx.get(idxB));
                        g = (int) (opacity * colorsG[classIdx] + o2 * idx.get(idxB+1));
                        r = (int) (opacity * colorsR[classIdx] + o2 * idx.get(idxB+2));
                    }

                    idx.put(idxB, b);
                    idx.put(idxB + 1, g);
                    idx.put(idxB + 2, r);
                }
            }

        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int classIdx = ig.get();
                    if(classIdx >= colorsB.length)
                        initColors(step.classColors(), colorsB.length + 32);

                    long idxB = (3 * w * y) + (3 * x);
                    idx.put(idxB, colorsB[classIdx]);
                    idx.put(idxB + 1, colorsG[classIdx]);
                    idx.put(idxB + 2, colorsR[classIdx]);
                }
            }
        }

        if(resizeRequired){
            Image im = data.getImage(imgName);
            BoundingBox bb = ImageToNDArray.getCropRegion(im, step.imageToNDArrayConfig());
            int oH = (int) (bb.height() * im.height());
            int oW = (int) (bb.width() * im.width());
            int x1 = (int) (bb.x1() * im.width());
            int y1 = (int) (bb.y1() * im.height());

            Mat resized = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.resize(drawOn, resized, new Size(oW, oH));

            //Now that we've resized - need to apply to the original image...
            //Note that to use accumulateWeighted we need to use a float type - method doesn't support integer types
            Mat resizedFloat = new Mat();
            resized.convertTo(resizedFloat, CvType.CV_32FC3);


            Mat asFloat = new Mat();
            im.getAs(Mat.class).convertTo(asFloat, CvType.CV_32FC3);
            Mat subset = asFloat.apply(new Rect(x1, y1, oW, oH));
            double opacity = step.opacity();
            org.bytedeco.opencv.global.opencv_imgproc.accumulateWeighted(resized, subset, opacity);
            Mat out = new Mat();
            asFloat.convertTo(out, CvType.CV_8UC3);
            drawOn = out;
        }


        String outputName = step.outputName();
        if(outputName == null)
            outputName = DrawSegmentationStep.DEFAULT_OUTPUT_NAME;

        return Data.singleton(outputName, Image.create(drawOn));
    }

    private void initColors(List<String> classColors, int max){
        if (colorsB == null && classColors != null) {
            colorsB = new int[classColors.size()];
            colorsG = new int[classColors.size()];
            colorsR = new int[classColors.size()];
            for (int i = 0; i < colorsB.length; i++) {
                Scalar c = ColorUtil.stringToColor(classColors.get(i));
                colorsB[i] = (int) c.blue();
                colorsG[i] = (int) c.green();
                colorsR[i] = (int) c.red();
            }
        }

        if(colorsB == null || colorsB.length < max){
            //Generate some random colors, because we don't have any labels, or enough labels
            int start;
            if(colorsB == null){
                colorsB = new int[max];
                colorsG = new int[max];
                colorsR = new int[max];
                start = 0;
            } else {
                start = colorsB.length;
                colorsB = Arrays.copyOf(colorsB, max);
                colorsG = Arrays.copyOf(colorsG, max);
                colorsR = Arrays.copyOf(colorsR, max);
            }
            Random rng = new Random(12345);
            if(start > 0){
                //Hack to advance RNG seed, so we get repeatability
                for( int i=0; i<start; i++){
                    rng.nextInt(255);
                    rng.nextInt(255);
                    rng.nextInt(255);
                }
            }

            for( int i=start; i<max; i++ ){
                Scalar s = ColorUtil.randomColor(rng);
                colorsB[i] = (int) s.blue();
                colorsG[i] = (int) s.green();
                colorsR[i] = (int) s.red();
            }
        }
    }

    private interface IntGetter {
        int get();
    }
}
