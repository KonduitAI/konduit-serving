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

package ai.konduit.serving.data.image.step.pose.openpose;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class DrawOpenPoseStepRunner implements PipelineStepRunner {

    protected final DrawOpenPoseStep step;

    public DrawOpenPoseStepRunner(@NonNull DrawOpenPoseStep step){
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

        String imgName = step.imageName();
        String ndName = step.ndarrayName();

        if (imgName == null) {
            String errMultipleKeys = "Image field name was not provided and could not be inferred: multiple image fields exist: %s and %s";
            String errNoKeys = "Image field name was not provided and could not be inferred: no image fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }
        if (ndName == null) {
            String errMultipleKeys = "NDArray field name was not provided and could not be inferred: multiple NDArray fields exist: %s and %s";
            String errNoKeys = "NDArray field name was not provided and could not be inferred: no NDArray fields exist";
            ndName = DataUtils.inferField(data, ValueType.NDARRAY, false, errMultipleKeys, errNoKeys);
        }

        Image i = data.getImage(imgName);
        NDArray n = data.getNDArray(ndName);



        SerializedNDArray snd = n.getAs(SerializedNDArray.class);
        NDArrayType t = snd.getType();
        if(t != NDArrayType.FLOAT){
            throw new IllegalStateException("Not yet implemented");
        }
        snd.getBuffer().rewind();
        FloatBuffer fb = snd.getBuffer().asFloatBuffer();       //Shape: [1, h, w, c]
        long[] shape = snd.getShape();
        int h = (int) shape[1];
        int w = (int) shape[2];
        int ch = (int) shape[3];

        int nP = ch;
        int[] maxX = new int[nP];
        int[] maxY = new int[nP];
        float[] max = new float[nP];

        for( int y=0; y<h; y++ ){
            for( int x=0; x<w; x++ ){
                for( int c=0; c<nP; c++ ){      //TODO other channels
                    float f = fb.get();
                    if(f > max[c]){
                        max[c] = f;
                        maxY[c] = y;
                        maxX[c] = x;
                    }
                }
            }
        }

        System.out.println("x: " + Arrays.toString(maxX));
        System.out.println("y: " + Arrays.toString(maxY));
        System.out.println("v: " + Arrays.toString(max));

        //Account for crop
        BoundingBox cropRegion = null;
        if(step.i2nConfig() != null){
            ImageToNDArrayConfig c = step.i2nConfig();
            cropRegion = ImageToNDArray.getCropRegion(i, c);
        }

        Mat m = i.getAs(Mat.class);
        for( int j=0; j<maxX.length; j++ ){
            double xFrac = maxX[j] / (double)w;
            double yFrac = maxY[j] / (double)h;

            if(cropRegion != null){
                xFrac = cropRegion.x1() + xFrac * cropRegion.width();
                yFrac = cropRegion.y1() + yFrac * cropRegion.height();
            }

            int xP = (int) (xFrac * i.width());
            int yP = (int) (yFrac * i.height());

            int step = 255 / maxX.length;
            Scalar color = new Scalar(j*step, j*step, j*step, 0);

//            drawPoint(m, Scalar.GREEN, 4, 2, xP, yP);
            drawPoint(m, color, 4, 2, xP, yP);
        }

        int[][] cocoPairs = new int[][]{
            {1, 2}, {1, 5}, {2, 3}, {3, 4}, {5, 6}, {6, 7}, {1, 8}, {8, 9}, {9, 10}, {1, 11},
            {11, 12}, {12, 13}, {1, 0}, {0, 14}, {14, 16}, {0, 15}, {15, 17}, {2, 16}, {5, 17}
        };

        for( int j=0; j<cocoPairs.length; j++ ){
            double x1Frac = maxX[cocoPairs[j][0]] / (double)w;
            double y1Frac = maxY[cocoPairs[j][0]] / (double)h;
            double x2Frac = maxX[cocoPairs[j][1]] / (double)w;
            double y2Frac = maxY[cocoPairs[j][1]] / (double)h;

            if(cropRegion != null){
                x1Frac = cropRegion.x1() + x1Frac * cropRegion.width();
                y1Frac = cropRegion.y1() + y1Frac * cropRegion.height();
                x2Frac = cropRegion.x1() + x2Frac * cropRegion.width();
                y2Frac = cropRegion.y1() + y2Frac * cropRegion.height();
            }

            int x1P = (int) (x1Frac * i.width());
            int y1P = (int) (y1Frac * i.height());
            int x2P = (int) (x2Frac * i.width());
            int y2P = (int) (y2Frac * i.height());

            drawLine(m, Scalar.GREEN, 3, x1P, x2P, y1P, y2P);
        }


        Data out = data.clone();
        Image outImg = Image.create(m);
        out.put(imgName, outImg);

        return out;
    }

    protected void drawPoint(Mat m, Scalar color, int radius, int thickness, int x, int y){
        Point p = new Point(x, y);
        int lineType = 8;
        int shift = 0;
        org.bytedeco.opencv.global.opencv_imgproc.circle(m, p, radius, color, thickness, lineType, shift);
    }

    protected void drawLine(Mat m, Scalar color, int thickness, int x1, int x2, int y1, int y2){
        Point p1 = new Point(x1, y1);
        Point p2 = new Point(x2, y2);
        int lineType = 8;
        int shift = 0;
        org.bytedeco.opencv.global.opencv_imgproc.line(m, p1, p2, color, thickness, lineType, shift);
    }
}
