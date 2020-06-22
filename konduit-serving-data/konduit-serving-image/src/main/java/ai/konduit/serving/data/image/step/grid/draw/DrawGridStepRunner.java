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

package ai.konduit.serving.data.image.step.grid.draw;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.ConvexHull2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.MonotoneChain;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

@CanRun({DrawGridStep.class, DrawFixedGridStep.class})
public class DrawGridStepRunner implements PipelineStepRunner {

    protected final DrawGridStep step;
    protected final DrawFixedGridStep fStep;

    public DrawGridStepRunner(@NonNull DrawGridStep step){
        this.step = step;
        this.fStep = null;
    }

    public DrawGridStepRunner(@NonNull DrawFixedGridStep step){
        this.step = null;
        this.fStep = step;
    }

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        if(step != null)
            return step;
        return fStep;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        boolean fixed = fStep != null;

        String imgName = fixed ? fStep.imageName() : step.imageName();

        if (imgName == null) {
            String errMultipleKeys = "DrawGridStep points field name was not provided and could not be inferred: multiple List<Point> fields exist: %s and %s";
            String errNoKeys = "DrawGridStep points field name was not provided and could not be inferred: no List<Point> fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

        Image i = data.getImage(imgName);
        List<Point> points;

        if(fixed){
            points = fStep.points();
        } else {
            String pName = step.pointsName();
            if (pName == null || pName.isEmpty()) {
                String errMultipleKeys = "DrawGridStep points field name was not provided and could not be inferred: multiple List<Point> fields exist: %s and %s";
                String errNoKeys = "DrawGridStep points field name was not provided and could not be inferred: no List<Point> fields exist";
                pName = DataUtils.inferListField(data, ValueType.POINT, errMultipleKeys, errNoKeys);
            }

            Preconditions.checkState(data.has(pName), "Error in CropGridStep: Input Data does not have any values for pointName=\"%s\"", pName);

            if (data.type(pName) != ValueType.LIST || data.listType(pName) != ValueType.POINT) {
                String type = (data.type(pName) == ValueType.LIST ? "List<" + data.listType(pName).toString() + ">" : "" + data.type(pName));
                throw new IllegalStateException("pointName = \"" + pName + "\" should be a length 4 List<Point> but is type " + type);
            }

            points = data.getListPoint(pName);
        }

        Preconditions.checkState(points != null && points.size() == 4, "Input List<Points> must have length 4, got %s", points);

        Mat m = i.getAs(Mat.class).clone();

        Scalar borderColor;
        int borderThickness;
        if(fixed){
            borderColor = fStep.borderColor() == null ? ColorUtil.stringToColor(DrawGridStep.DEFAULT_COLOR) : ColorUtil.stringToColor(fStep.borderColor());
            borderThickness = fStep.borderThickness();
        } else {
            borderColor = step.borderColor() == null ? ColorUtil.stringToColor(DrawGridStep.DEFAULT_COLOR) : ColorUtil.stringToColor(step.borderColor());
            borderThickness = step.borderThickness();
        }
        if(borderThickness <= 0)
            borderThickness = 1;


        List<Point> pixelPoints;
        if(fixed && fStep.coordsArePixels() || !fixed && step.coordsArePixels()){
            pixelPoints = points;
        } else {
            //Fraction points
            pixelPoints = new ArrayList<>(4);
            for(Point p : points){
                pixelPoints.add(Point.create(p.x() * i.width(), p.y() * i.height()));
            }
        }

        //Draw border:
        drawLine(m, borderColor, borderThickness, pixelPoints.get(0), pixelPoints.get(1));   //TL -> TR
        drawLine(m, borderColor, borderThickness, pixelPoints.get(0), pixelPoints.get(2));   //TL -> BL
        drawLine(m, borderColor, borderThickness, pixelPoints.get(1), pixelPoints.get(3));   //TR -> BR
        drawLine(m, borderColor, borderThickness, pixelPoints.get(2), pixelPoints.get(3));   //BL -> BR

        Scalar gridColor;
        int gridThickness;
        if(fixed){
            gridColor = fStep.gridColor() == null ? borderColor : ColorUtil.stringToColor(fStep.gridColor());
            gridThickness = fStep.gridThickness() == null ? borderThickness : fStep.gridThickness();
        } else {
            gridColor = step.gridColor() == null ? borderColor : ColorUtil.stringToColor(step.gridColor());
            gridThickness = step.gridThickness() == null ? borderThickness : step.gridThickness();
        }

        if(gridThickness <= 0)
            gridThickness = 1;

        int gridX = fixed ? fStep.gridX() : step.gridX();
        int gridY = fixed ? fStep.gridY() : step.gridY();
        drawGrid(m, gridColor, gridThickness, pixelPoints, gridX, gridY);

        Data out = data.clone();
        Image outImg = Image.create(m);
        out.put(imgName, outImg);

        return out;
    }

    protected void drawLine(Mat m, Scalar color, int thickness, Point p1, Point p2){
        org.bytedeco.opencv.opencv_core.Point pa = new org.bytedeco.opencv.opencv_core.Point((int)p1.x(), (int)p1.y());
        org.bytedeco.opencv.opencv_core.Point pb = new org.bytedeco.opencv.opencv_core.Point((int)p2.x(), (int)p2.y());
        int lineType = 8;
        int shift = 0;
        org.bytedeco.opencv.global.opencv_imgproc.line(m, pa, pb, color, thickness, lineType, shift);
    }

    protected void drawLine(Mat m, Scalar color, int thickness, int x1, int x2, int y1, int y2){
        org.bytedeco.opencv.opencv_core.Point p1 = new org.bytedeco.opencv.opencv_core.Point(x1, y1);
        org.bytedeco.opencv.opencv_core.Point p2 = new org.bytedeco.opencv.opencv_core.Point(x2, y2);
        int lineType = 8;
        int shift = 0;
        org.bytedeco.opencv.global.opencv_imgproc.line(m, p1, p2, color, thickness, lineType, shift);
    }


    protected void drawGrid(Mat m, Scalar color, int thickness, List<Point> pxPoints, int gridX, int gridY) {
        drawGridLines(m, pxPoints, false, gridX, color, thickness);
        drawGridLines(m, pxPoints, true, gridY, color, thickness);
    }

    protected void drawGridLines(Mat m, List<Point> pxPoints, boolean horizontalLines, int num, Scalar color, int thickness){
        Point p1a, p1b, p2a, p2b;
        if(horizontalLines){
            //Horizontal lines - perpendicular to (TL, BL) and (TR, BR)
            p1a = pxPoints.get(0);
            p1b = pxPoints.get(2);
            p2a = pxPoints.get(1);
            p2b = pxPoints.get(3);
        } else {
            //Vertical lines - perpendicular to (TL, TR) and (BL, BR)
            p1a = pxPoints.get(0);
            p1b = pxPoints.get(1);
            p2a = pxPoints.get(2);
            p2b = pxPoints.get(3);
        }



        for( int j=1; j<num; j++ ){
            double frac = j / (double)num;
            double deltaX1 = p1b.x()-p1a.x();
            double deltaX2 = p2b.x()-p2a.x();
            double deltaY1 = p1b.y()-p1a.y();
            double deltaY2 = p2b.y()-p2a.y();
            int x1Px = (int) (p1a.x() + frac * deltaX1);
            int x2Px = (int) (p2a.x() + frac * deltaX2);
            int y1Px = (int) (p1a.y() + frac * deltaY1);
            int y2Px = (int) (p2a.y() + frac * deltaY2);
            drawLine(m, color, thickness, x1Px, x2Px, y1Px, y2Px);
        }
    }
}
