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
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

@CanRun(value = DrawGridStep.class, moduleName = "konduit-serving-image")
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
            String errMultipleKeys = "Image field name was not provided and could not be inferred: multiple image fields exist: %s and %s";
            String errNoKeys = "Image field name was not provided and could not be inferred: no image fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

        Image i = data.getImage(imgName);
        double[] x;
        double[] y;

        if(fixed){
            x = fStep.x();
            y = fStep.y();
        } else {
            String xName = step.xName();
            String yName = step.yName();
            if (xName == null || yName == null) {
                throw new IllegalStateException("xName and yName must be specified in configuration. These should be the name of length 4 List<Double> or List<Long>");
            }

            if (data.type(xName) != ValueType.LIST || (data.listType(xName) != ValueType.DOUBLE && data.listType(xName) != ValueType.INT64)) {
                String str = (data.type(xName) == ValueType.LIST ? ", list type " + data.listType(xName).toString() : "");
                throw new IllegalStateException("xName = \"" + xName + "\" should be a length 4 List<Double> or List<Long>, but is type " + data.type(xName) + str);
            }

            if (data.type(yName) != ValueType.LIST || (data.listType(yName) != ValueType.DOUBLE && data.listType(yName) != ValueType.INT64)) {
                String str = (data.type(yName) == ValueType.LIST ? ", list type " + data.listType(yName).toString() : "");
                throw new IllegalStateException("yName = \"" + yName + "\" should be a length 4 List<Double> or List<Long>, but is type " + data.type(yName) + str);
            }

            x = getAsDouble("xName", xName, data);
            y = getAsDouble("yName", yName, data);
        }

        ConvexHull2D ch = convexHull(x, y);

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



        Segment[] segments = ch.getLineSegments();
        for (Segment s : segments) {
            Vector2D start = s.getStart();
            Vector2D end = s.getEnd();

            int x1Px, x2Px, y1Px, y2Px;
            if(fixed && fStep.coordsArePixels() || !fixed && step.coordsArePixels()){
                x1Px = (int) start.getX();
                x2Px = (int) end.getX();
                y1Px = (int) start.getY();
                y2Px = (int) end.getY();
            } else {
                x1Px = (int) (start.getX() * i.width());
                x2Px = (int) (end.getX() * i.width());
                y1Px = (int) (start.getY() * i.height());
                y2Px = (int) (end.getY() * i.height());
            }



            drawLine(m, borderColor, borderThickness, x1Px, x2Px, y1Px, y2Px);
        }

        Scalar gridColor;
        int gridThickness;
        if(fixed){
            gridColor = fStep.gridColor() == null ? borderColor : ColorUtil.stringToColor(fStep.gridColor());
            gridThickness = fStep.gridThickness();
        } else {
            gridColor = step.gridColor() == null ? borderColor : ColorUtil.stringToColor(step.gridColor());
            gridThickness = step.gridThickness();
        }

        if(gridThickness <= 0)
            gridThickness = 1;

        drawGrid(m, gridColor, gridThickness, segments, x, y);

        Data out = data.clone();
        Image outImg = Image.create(m);
        out.put(imgName, outImg);

        return out;
    }

    protected void drawLine(Mat m, Scalar color, int thickness, int x1, int x2, int y1, int y2){
        Point p1 = new Point(x1, y1);
        Point p2 = new Point(x2, y2);
        int lineType = 8;
        int shift = 0;
        org.bytedeco.opencv.global.opencv_imgproc.line(m, p1, p2, color, thickness, lineType, shift);
    }

    protected void drawGrid(Mat m, Scalar color, int thickness, Segment[] segments, double[] x, double[] y) {
        Segment grid1Segment1 = null;

        //Work out the
        for (Segment s : segments) {
            Vector2D start = s.getStart();
            Vector2D end = s.getEnd();
            if (grid1Segment1 == null &&
                    (start.getX() == x[0] && end.getX() == x[1]) ||
                    (start.getX() == x[1] && end.getX() == x[0])) {
                grid1Segment1 = s;
            }
        }

        if(grid1Segment1 == null){
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid order for grid points:");
            for( int i=0; i<4; i++ ){
                if(i > 0)
                    sb.append(",");
                sb.append(" (").append(x[i]).append(",").append(y[i]).append(")");
            }
            sb.append(" - first two points are on opposite corners of the grid, hence defining grid1 relative to this is impossible." +
                    " Box coordinates must be defined such that (x[0],y[0]) and (x[1],y[1]) are adjacent");

            throw new IllegalStateException(sb.toString());
        }

        Segment grid1Segment2 = null;
        Segment grid2Segment1 = null;
        Segment grid2Segment2 = null;
        for (Segment s : segments) {
            if (s == grid1Segment1)
                continue;
            if (!grid1Segment1.getStart().equals(s.getStart()) &&
                    !grid1Segment1.getStart().equals(s.getEnd()) &&
                    !grid1Segment1.getEnd().equals(s.getStart()) &&
                    !grid1Segment1.getEnd().equals(s.getEnd())) {
                grid1Segment2 = s;
            } else if (grid2Segment1 == null) {
                grid2Segment1 = s;
            } else {
                grid2Segment2 = s;
            }
        }

        int g1, g2;
        if(step != null){
            g1 = step.grid1();
            g2 = step.grid2();
        } else {
            g1 = fStep.grid1();
            g2 = fStep.grid2();
        }

        drawGridLines(m, grid1Segment1, grid1Segment2, g1, color, thickness);
        drawGridLines(m, grid2Segment1, grid2Segment2, g2, color, thickness);
    }

    protected void drawGridLines(Mat m, Segment s1, Segment s2, int num, Scalar color, int thickness){
        double x1, x2, x3, x4, y1, y2, y3, y4;
        if(s1.getStart().getX() <= s1.getEnd().getX()){
            x1 = s1.getStart().getX();
            x2 = s1.getEnd().getX();
            y1 = s1.getStart().getY();
            y2 = s1.getEnd().getY();
        } else {
            x1 = s1.getEnd().getX();
            x2 = s1.getStart().getX();
            y1 = s1.getEnd().getY();
            y2 = s1.getStart().getY();
        }

        if(s2.getStart().getX() <= s2.getEnd().getX()){
            x3 = s2.getStart().getX();
            x4 = s2.getEnd().getX();
            y3 = s2.getStart().getY();
            y4 = s2.getEnd().getY();
        } else {
            x3 = s2.getEnd().getX();
            x4 = s2.getStart().getX();
            y3 = s2.getEnd().getY();
            y4 = s2.getStart().getY();
        }

        boolean s1XMostIsTop = y1 < y2;
        boolean s2XMostIsTop = y3 < y4;

        if(s1XMostIsTop != s2XMostIsTop){
            //Need to swap
            double tx3 = x3;
            double ty3 = y3;
            x3 = x4;
            y3 = y4;
            x4 = tx3;
            y4 = ty3;
        }


        for( int j=1; j<num; j++ ){
            double frac = j / (double)num;
            double deltaX1 = x2-x1;
            double deltaX2 = x4-x3;
            double deltaY1 = y2-y1;
            double deltaY2 = y4-y3;
            int x1Px, x2Px, y1Px, y2Px;
            if((step != null && step.coordsArePixels()) || (fStep != null && fStep.coordsArePixels())){
                x1Px = (int) (x1 + frac * deltaX1);
                x2Px = (int) (x3 + frac * deltaX2);
                y1Px = (int) (y1 + frac * deltaY1);
                y2Px = (int) (y3 + frac * deltaY2);
            } else {
                x1Px = (int) (m.cols() * (x1 + frac * deltaX1));
                x2Px = (int) (m.cols() * (x3 + frac * deltaX2));
                y1Px = (int) (m.rows() * (y1 + frac * deltaY1));
                y2Px = (int) (m.rows() * (y3 + frac * deltaY2));
            }
            drawLine(m, color, thickness, x1Px, x2Px, y1Px, y2Px);
        }
    }

    protected double[] getAsDouble(String label, String xyName, Data data){
        if(data.listType(xyName) == ValueType.DOUBLE){
            List<Double> l = data.getListDouble(xyName);
            Preconditions.checkState(l.size() == 4, label + "=" + xyName + " should be a length 4 list but is length " + l.size());
            double[] xy = new double[4];
            for( int j=0; j<4; j++ ){
                xy[j] = l.get(j);
            }
            return xy;
        } else {
            throw new UnsupportedOperationException("Not yet implemeted - int64");
        }
    }

    protected ConvexHull2D convexHull(double[] x, double[] y){
        List<Vector2D> vList = new ArrayList<>(4);
        for(int i=0; i<4; i++ ){
            vList.add(new Vector2D(x[i], y[i]));
        }
        return new MonotoneChain().generate(vList);
    }
}
