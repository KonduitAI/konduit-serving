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

package ai.konduit.serving.data.image.step.point.draw;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.opencv.core.CvType;

import java.util.*;

@CanRun(DrawPointsStep.class)
public class DrawPointsRunner implements PipelineStepRunner {

    protected final DrawPointsStep step;
    protected Map<String, Scalar> labelMap;

    public DrawPointsRunner(@NonNull DrawPointsStep step) {
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

        if(step.points() == null || step.points().size() == 0){
            throw new IllegalArgumentException("No point input data fields defined. Nothing to draw.");
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
                points.add(point);
            }else if(type == ValueType.LIST){
                List<Point> pointList = data.getListPoint(pointName);
                for (Point point : pointList) {
                    if(point.dimensions() != 2){
                        throw new IllegalArgumentException("Point in input "+pointName+" has "+point.dimensions()+" dimensions, but only 2 dimensional points are supported for drawing!");
                    }
                }
                points.addAll(pointList);
            }else {
                throw new IllegalArgumentException("The configured input "+pointName+" is neither a point nor a list of points!");
            }
        }

        // get reference size and initialize image
        int width;
        int height;
        Mat image;
        if(step.image() != null){
            ValueType type = data.type(step.image());
            if(type == ValueType.IMAGE){
                Image img = data.getImage(step.image());
                width = img.width();
                height = img.height();
                image = img.getAs(Mat.class);
            }else{
                throw new IllegalArgumentException("The configured reference image input "+step.image()+" is not an Image!");
            }
        }else if(step.width() != null && step.height() != null){
            width = step.width();
            height = step.height();
            image = new Mat();
            image.put(Mat.zeros(height, width, CvType.CV_8UC3));
        }else{
            throw new IllegalArgumentException("You have to provide either a reference image or width AND height!");
        }

        // turn points with relative addressing to absolute addressing
        List<Point> absPoints = new ArrayList<>(points.size());
        for (Point point : points) {
            absPoints.add(accountForCrop(point, width, height, step.imageToNDArrayConfig()));
        }

        // draw points on image with color according to labels
        int radius = step.radius() == null ? 5 : step.radius();
        for (Point point : absPoints) {
            Scalar color;
            if(point.label() == null){
                if(step.noClassColor() == null){
                    color = ColorUtil.stringToColor(DrawPointsStep.DEFAULT_NO_POINT_COLOR);
                } else {
                    color = ColorUtil.stringToColor(step.noClassColor());
                }
            } else {
                // Initialize colors first if they weren't initialized at all
                if(labelMap == null) {
                    Map<String, String> classColors = step.classColors();
                    if(classColors == null){
                        throw new IllegalArgumentException("A label to color configuration has to be passed!");
                    }
                    initColors(classColors, classColors.size());
                }
                color = labelMap.get(point.label());
                if(color == null){
                    throw new IllegalArgumentException("No color provided for label " + point.label());
                }
            }

            opencv_imgproc.circle(
                    image,
                    new org.bytedeco.opencv.opencv_core.Point((int)point.x(), (int)point.y()),
                    radius,
                    color,
                    opencv_imgproc.FILLED,
                    opencv_imgproc.LINE_AA,
                    0
            );
        }

        // return image
        out.put(step.outputName() == null ? DrawPointsStep.DEFAULT_OUTPUT_NAME : step.outputName(), Image.create(image));
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

    private void initColors(Map<String, String> classColors, int max) {
        labelMap = new HashMap<>(classColors.size());
        for (Map.Entry<String, String> entry : classColors.entrySet()) {
            labelMap.put(entry.getKey(), ColorUtil.stringToColor(entry.getValue()));
        }
    }
}
