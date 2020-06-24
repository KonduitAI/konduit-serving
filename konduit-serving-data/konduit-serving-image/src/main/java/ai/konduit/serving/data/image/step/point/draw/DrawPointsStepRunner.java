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

package ai.konduit.serving.data.image.step.point.draw;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.opencv.core.CvType;

import java.util.*;

@CanRun(DrawPointsStep.class)
public class DrawPointsStepRunner implements PipelineStepRunner {

    protected final DrawPointsStep step;
    protected Map<String, Scalar> labelMap;

    public DrawPointsStepRunner(@NonNull DrawPointsStep step) {
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
        // TODO: Ask Alex about consuming and non consuming Steps. It seems to me that unused inputs should
        //       always go through unchanged
        // TODO: Ask Alex about lombok val usage. What style is preferred?
        Data out = Data.empty();
        for(String key : data.keys()){
            out.copyFrom(key, data);
        }

        // collect points and how many classes there are
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
        // Initialize colors first if they weren't initialized at all
        if(labelMap == null) {
            Map<String, String> classColors = step.classColors();
            initColors(classColors, classColors.size());
        }

        // get reference size
        int width;
        int height;
        if(step.image() != null){
            ValueType type = data.type(step.image());
            if(type == ValueType.IMAGE){
                Image image = data.getImage(step.image());
                width = image.width();
                height = image.height();
            }else{
                throw new IllegalArgumentException("The configured reference image input "+step.image()+" is not an Image!");
            }
        }else if(step.width() != null && step.height() != null){
            width = step.width();
            height = step.height();
        }else{
            throw new IllegalArgumentException("You have to provide either a reference image or width AND height!");
        }

        // turn points with relative addressing to absolute addressing
        List<Point> absPoints = new ArrayList<>(points.size());
        for (Point point : points) {
            absPoints.add(point.toAbsolute(width, height));
        }

        // create empty image
        Mat image = new Mat();
        image.put(Mat.zeros(height, width, CvType.CV_8UC3));

        // draw points on image with color according to labels
        int radius = step.radius() == null ? 5 : step.radius();
        for (Point point : absPoints) {
            Scalar color = labelMap.get(point.label());
            if(color == null){
                throw new IllegalArgumentException("No color provided for label "+point.label());
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

    private void initColors(Map<String, String> classColors, int max) {
        labelMap = new HashMap<>(classColors.size());
        for (Map.Entry<String, String> entry : classColors.entrySet()) {
            labelMap.put(entry.getKey(), ColorUtil.stringToColor(entry.getValue()));
        }
    }
}
