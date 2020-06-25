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

package ai.konduit.serving.data.image.step.point.heatmap;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.opencv.core.CvType;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@CanRun(DrawHeatmapStep.class)
public class DrawHeatmapRunner implements PipelineStepRunner {

    protected final DrawHeatmapStep step;
    protected Mat prev;

    public DrawHeatmapRunner(@NonNull DrawHeatmapStep step) {
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
        Data out = Data.empty();
        for(String key : data.keys()){
            out.copyFrom(key, data);
        }

        // get reference size
        int width;
        int height;
        Mat targetImage = null;
        if(step.image() != null){
            ValueType type = data.type(step.image());
            if(type == ValueType.IMAGE){
                Image image = data.getImage(step.image());
                width = image.width();
                height = image.height();
                targetImage = image.getAs(Mat.class);
            }else{
                throw new IllegalArgumentException("The configured reference image input "+step.image()+" is not an Image!");
            }
        }else if(step.width() != null && step.height() != null){
            width = step.width();
            height = step.height();
        }else{
            throw new IllegalArgumentException("You have to provide either a reference image or width AND height!");
        }

        if(prev == null){
            prev = new Mat();
            prev.put(Mat.zeros(height, width, CvType.CV_64FC1));
        }

        // collect points
        List<Point> points = new LinkedList<>();
        for (String pointName : step.points()) {
            ValueType type = data.type(pointName);
            if(type == ValueType.POINT){
                Point point = data.getPoint(pointName);
                if(point.dimensions() != 2){
                    throw new IllegalArgumentException("Point in input "+pointName+" has "+point.dimensions()+" dimensions, but only 2 dimensional points are supported for drawing!");
                }
                points.add(accountForCrop(point, width, height, step.imageToNDArrayConfig()));
            }else if(type == ValueType.LIST){
                List<Point> pointList = data.getListPoint(pointName);
                for (Point point : pointList) {
                    if(point.dimensions() != 2){
                        throw new IllegalArgumentException("Point in input "+pointName+" has "+point.dimensions()+" dimensions, but only 2 dimensional points are supported for drawing!");
                    }
                    points.add(accountForCrop(point, width, height, step.imageToNDArrayConfig()));
                }
            }else {
                throw new IllegalArgumentException("The configured input "+pointName+" is neither a point nor a list of points!");
            }
        }

        Mat mat = new Mat();
        mat.put(Mat.zeros(height, width, CvType.CV_64FC1));
        DoubleIndexer idx = mat.createIndexer();
        for (Point point : points) {
            int row = (int) point.y();
            int col = (int) point.x();
            if(row > height || col > width){
                log.warn("{} is out of bounds ({}, {})", point, width, height);
            }else {
                idx.put(row, col, idx.get(row, col) + 255);
            }
        }

        int radius = step.radius() == null ? 15 : step.radius();
        int kSize = radius * 8 + 1;

        Size kernelSize = new Size(kSize, kSize);
        opencv_imgproc.GaussianBlur(mat, mat, kernelSize, radius, radius, opencv_core.BORDER_ISOLATED);

        opencv_core.addWeighted(prev, step.fadingFactor() == null ? 0.9 : step.fadingFactor(), mat, 1.0, 0, mat);
        prev.close();
        prev = mat;

        DoublePointer maxVal = new DoublePointer(1);
        opencv_core.minMaxLoc(mat, null, maxVal, null, null, null);

        Mat scaledOut = new Mat();
        mat.convertTo(scaledOut, CvType.CV_8UC1, 255/maxVal.get(), 0);
        maxVal.close();

        Mat image = new Mat();
        opencv_imgproc.applyColorMap(scaledOut, image, opencv_imgproc.COLORMAP_TURBO);

        // return image
        Image outputImage;
        if(targetImage == null){
            outputImage = Image.create(image);
        }else{
            Mat composed = new Mat();
            opencv_core.addWeighted(targetImage, 1.0, image, step.opacity() == null ? 0.5 : step.opacity(), 0, composed);
            outputImage = Image.create(composed);
        }
        out.put(step.outputName() == null ? DrawHeatmapStep.DEFAULT_OUTPUT_NAME : step.outputName(), outputImage);
        return out;
    }

    private Point accountForCrop(Point relPoint, int width, int height, ImageToNDArrayConfig imageToNDArrayConfig) {
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
}
