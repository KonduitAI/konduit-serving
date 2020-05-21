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

package ai.konduit.serving.data.image.step.grid.crop;

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
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

public class CropGridStepRunner implements PipelineStepRunner {



    protected final CropGridStep step;
    protected final CropFixedGridStep fStep;

    public CropGridStepRunner(@NonNull CropGridStep step){
        this.step = step;
        this.fStep = null;
    }

    public CropGridStepRunner(@NonNull CropFixedGridStep step){
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

        Mat m = i.getAs(Mat.class);


        Vector2D[] corners = ch.getVertices();


        Segment[] segments = ch.getLineSegments();
        List<Image> l = cropGrid(m, segments, x, y);

        Data out = data.clone();
        out.putListImage("crops", l);

        return out;
    }

    protected List<Image> cropGrid(Mat m, Segment[] segments, double[] x, double[] y) {
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

        /*
        At this point
        grid1Segment1 is the grid1 left side of the grid area
        grid1Segment2 is the grid1 right side of the grid area
        grid2Segment1 is the top side of the grid area
        grid2Segment2 is the bottom side of the grid area
         */

        //Corner coordinates
        double x1, x2, x3, x4, y1, y2, y3, y4;
        if(grid1Segment1.getStart().getX() <= grid1Segment1.getEnd().getX()){
            x1 = grid1Segment1.getStart().getX();
            x2 = grid1Segment1.getEnd().getX();
            y1 = grid1Segment1.getStart().getY();
            y2 = grid1Segment1.getEnd().getY();
        } else {
            x1 = grid1Segment1.getEnd().getX();
            x2 = grid1Segment1.getStart().getX();
            y1 = grid1Segment1.getEnd().getY();
            y2 = grid1Segment1.getStart().getY();
        }
        if(grid1Segment2.getStart().getX() <= grid1Segment2.getEnd().getX()){
            x3 = grid1Segment2.getStart().getX();
            x4 = grid1Segment2.getEnd().getX();
            y3 = grid1Segment2.getStart().getY();
            y4 = grid1Segment2.getEnd().getY();
        } else {
            x3 = grid1Segment2.getEnd().getX();
            x4 = grid1Segment2.getStart().getX();
            y3 = grid1Segment2.getEnd().getY();
            y4 = grid1Segment2.getStart().getY();
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




//        double dX1 = (x2-x1)/g1;
//        double dX2 = (x4-x3)/g1;
//        double dY1 = (y2-y1)/g2;
//        double dY2 = (y4-y3)/g2;

        double dX1 = (x2-x1)/g2;
        double dX2 = (x4-x3)/g2;
        double dY1 = (y2-y1)/g1;
        double dY2 = (y4-y3)/g1;

        List<Image> out = new ArrayList<>();
        for( int i=0; i<g1; i++ ){

            int bx1 = (int) (m.cols() * (x1 + i*dX1));
            int bx2 = (int) (m.cols() * (x1 + (i+1)*dX1));
            int by1 = (int) (m.rows() * (y1 + i*dY1));
            int by2 = (int) (m.rows() * (y1 + (i+1)*dY1));

            int bx3 = (int) (m.cols() * (x3 + i*dX2));
            int bx4 = (int) (m.cols() * (x3 + (i+1)*dX2));
            int by3 = (int) (m.rows() * (y3 + i*dY2));
            int by4 = (int) (m.rows() * (y3 + (i+1)*dY2));

            for( int j=0; j<g2; j++ ){
                //Now, we need to segment again, to get the grid square
                double sg1 = 1.0 / g1;
                double sg2 = 1.0 / g2;
//                int ax1 = (int) (bx1 + j * sg2 * (bx2-bx1));
//                int ax2 = (int) (bx1 + j * sg2 * (bx2-bx1));
//                int ay1 = (int) (by1 + j * sg1 * (by2-by1));
//                int ay2 = (int) (by1 + j * sg1 * (by2-by1));
//
//                int ax3 = (int) (bx1 + (j+1) * sg2 * (bx3-bx1));
//                int ax4 = (int) (bx1 + (j+1) * sg2 * (bx4-bx2));
//                int ay3 = (int) (by1 + (j+1) * sg1 * (by3-by1));
//                int ay4 = (int) (by1 + (j+1) * sg1 * (by4-by2));

                double[] t1 = fracBetween(j*sg2, bx1, by1, bx3, by3);
                int ax1 = (int) t1[0];
                int ay1 = (int) t1[1];

                double[] t2 = fracBetween((j+1)*sg2, bx2, by2, bx4, by4);
                int ax2 = (int) t2[0];
                int ay2 = (int) t2[1];

                double[] t3 = fracBetween((j+1)*sg2, bx1, by1, bx3, by3);
                int ax3 = (int) t3[0];
                int ay3 = (int) t3[1];

                double[] t4 = fracBetween((j+1)*sg2, bx2, by2, bx4, by4);
                int ax4 = (int) t4[0];
                int ay4 = (int) t4[1];

                //For now, we'll select the square around these:
//                int minX = min(bx1, bx2, bx3, bx4);
//                int maxX = max(bx1, bx2, bx3, bx4);
//                int minY = min(by1, by2, by3, by4);
//                int maxY = max(by1, by2, by3, by4);

                int minX = min(ax1, ax2, ax3, ax4);
                int maxX = max(ax1, ax2, ax3, ax4);
                int minY = min(ay1, ay2, ay3, ay4);
                int maxY = max(ay1, ay2, ay3, ay4);

                int w = maxX-minX;
                int h = maxY-minY;
                Rect r = new Rect(minX, minY, w, h);
                Mat crop = m.apply(r);
                out.add(Image.create(crop));
            }
        }


        return out;
    }

    private double[] fracBetween(double frac, double x1, double y1, double x2, double y2){
        return new double[]{
                x1 + frac * (x2-x1),
                y1 + frac * (y2-y1)};
    }

    private int min(int a, int b, int c, int d){
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private int max(int a, int b, int c, int d){
        return Math.max(Math.max(a, b), Math.max(c, d));
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
