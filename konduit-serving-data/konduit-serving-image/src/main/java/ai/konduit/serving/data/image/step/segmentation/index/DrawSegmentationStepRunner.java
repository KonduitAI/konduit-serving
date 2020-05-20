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

import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.NonNull;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.opencv.core.CvType;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class DrawSegmentationStepRunner implements PipelineStepRunner {

    protected final DrawSegmentationStep step;
    protected Scalar[] colors;
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
        List<String> classColors = step.classColors();
        if (colors == null && classColors != null) {
            colors = new Scalar[classColors.size()];
            colorsB = new int[classColors.size()];
            colorsG = new int[classColors.size()];
            colorsR = new int[classColors.size()];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = ColorUtil.stringToColor(classColors.get(i));
                int r = (int) colors[i].red();
                int g = (int) colors[i].green();
                int b = (int) colors[i].blue();
                colorsB[i] = b;
                colorsG[i] = g;
                colorsR[i] = r;
            }
        }

        NDArray mask = data.getNDArray(step.segmentArray());

        Mat drawOn;
        String imgName = step.image();

        if (imgName == null) {
            long[] shape = mask.shape();
            //TODO checks for rank and shape
            drawOn = new Mat((int) shape[1], (int) shape[2], CvType.CV_8UC3);       //8 bits per chanel RGB
        } else {
            Image i = data.getImage(imgName);
            drawOn = new Mat();
            i.getAs(Mat.class).clone().convertTo(drawOn, CvType.CV_8UC3);
        }

        SerializedNDArray nd = mask.getAs(SerializedNDArray.class);
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


        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int classIdx = ig.get();
                int idxB = (3 * w * y) + (3 * x);
                idx.put(idxB, colorsB[classIdx]);
                idx.put(idxB + 1, colorsG[classIdx]);
                idx.put(idxB + 2, colorsR[classIdx]);
            }
        }


        String outputName = step.outputName();
        if(outputName == null)
            outputName = DrawSegmentationStep.DEFAULT_OUTPUT_NAME;

        return Data.singleton(outputName, Image.create(drawOn));
    }

    private interface IntGetter {
        int get();
    }
}
