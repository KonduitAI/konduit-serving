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

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ColorUtil;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
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
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DrawGridStepRunner implements PipelineStepRunner {

    protected static final String INVALID_COLOR = "Invalid color: Must be in one of the following formats: hex/HTML - #788E87, " +
            "RGB - rgb(128,0,255), or a color such as \"green\", etc - got \"%s\"";




    protected final DrawGridStep step;

    public DrawGridStepRunner(@NonNull DrawGridStep step){
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
        String xName = step.xName();
        String yName = step.yName();

        if (imgName == null) {
            String errMultipleKeys = "Image field name was not provided and could not be inferred: multiple image fields exist: %s and %s";
            String errNoKeys = "Image field name was not provided and could not be inferred: no image fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

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


        Image i = data.getImage(imgName);
        double[] x = getAsDouble("xName", xName, data);
        double[] y = getAsDouble("yName", yName, data);

        ConvexHull2D ch = convexHull(x, y);

        Mat m = i.getAs(Mat.class).clone();

        Scalar borderColor = step.borderColor() == null ? ColorUtil.stringToColor(DrawGridStep.DEFAULT_COLOR) : ColorUtil.stringToColor(step.borderColor());
        int borderThickness = step.borderThickness();
        if(borderThickness <= 0)
            borderThickness = 1;



        Segment[] segments = ch.getLineSegments();
        for (Segment s : segments) {
            Vector2D start = s.getStart();
            Vector2D end = s.getEnd();

            int x1Px = (int) (start.getX() * i.width());
            int x2Px = (int) (end.getX() * i.width());
            int y1Px = (int) (start.getY() * i.height());
            int y2Px = (int) (end.getY() * i.height());

            drawLine(m, borderColor, borderThickness, x1Px, x2Px, y1Px, y2Px);
        }

        Scalar gridColor = step.gridColor() == null ? borderColor : ColorUtil.stringToColor(step.gridColor());
        int gridThickness = step.gridThickness();
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

        for (Segment s : segments) {
            Vector2D start = s.getStart();
            Vector2D end = s.getEnd();
            if (grid1Segment1 == null &&
                    (start.getX() == x[0] && end.getX() == x[1]) ||
                    (start.getX() == x[1] && end.getX() == x[0])) {
                grid1Segment1 = s;
            }
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

        drawGridLines(m, grid1Segment1, grid1Segment2, step.grid1(), color, thickness);
        drawGridLines(m, grid2Segment1, grid2Segment2, step.grid2(), color, thickness);
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


        for( int j=1; j<num; j++ ){
            double frac = j / (double)num;
            double deltaX1 = x2-x1;
            double deltaX2 = x4-x3;
            double deltaY1 = y2-y1;
            double deltaY2 = y4-y3;
            int x1Px = (int) (m.cols() * (x1 + frac * deltaX1));
            int x2Px = (int) (m.cols() * (x3 + frac * deltaX2));
            int y1Px = (int) (m.rows() * (y1 + frac * deltaY1));
            int y2Px = (int) (m.rows() * (y3 + frac * deltaY2));
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
