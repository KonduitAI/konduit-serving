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

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.util.ArrayList;
import java.util.List;

@CanRun({CropGridStep.class, CropFixedGridStep.class})
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
        List<Point> points;

        if(fixed){
            points = fStep.points();
            Preconditions.checkState(points != null, "Error in CropFixedGridStep: points field was null (corder points" +
                    " must be provided for cropping via CropFixedGridStep.points field)");
        } else {
            String pName = step.pointsName();
            if (pName == null) {
                String errMultipleKeys = "CropGridStep pointsName field name was not provided and could not be inferred: multiple List<Point> fields exist: %s and %s";
                String errNoKeys = "CropGridStep pointsName field name was not provided and could not be inferred: no List<Point> fields exist";
                pName = DataUtils.inferListField(data, ValueType.POINT, errMultipleKeys, errNoKeys);
            }

            Preconditions.checkState(data.has(pName), "Error in CropGridStep: Input Data does not have any values for pointName=\"%s\"", pName);

            if (data.type(pName) != ValueType.LIST || data.listType(pName) != ValueType.POINT) {
                String type = (data.type(pName) == ValueType.LIST ? "List<" + data.listType(pName).toString() + ">" : "" + data.type(pName));
                throw new IllegalStateException("pointName = \"" + pName + "\" should be a length 4 List<Point> but is type " + type);
            }
            points = data.getListPoint(pName);
        }

        boolean isPx = fixed ? fStep.coordsArePixels() : step.coordsArePixels();
        List<Point> pxPoints;
        if(isPx){
            pxPoints = points;
        } else {
            pxPoints = new ArrayList<>(4);
            for(Point p : points){
                pxPoints.add(Point.create(p.x() * i.width(), p.y() * i.height()));
            }
        }


        Mat m = i.getAs(Mat.class);
        double gx = fixed ? fStep.gridX() : step.gridX();
        double gy = fixed ? fStep.gridY() : step.gridY();
        Pair<List<Image>,List<BoundingBox>> p = cropGrid(m, pxPoints, gx, gy);

        Data out;
        if(step != null ? step.keepOtherFields() : fStep.keepOtherFields()){
            out = data.clone();
        } else {
            out = Data.empty();
        }

        String outName = (step != null ? step.outputName() : fStep.outputName());
        if(outName == null)
            outName = CropGridStep.DEFAULT_OUTPUT_NAME;
        out.putListImage(outName, p.getFirst());

        if(step != null ? step.boundingBoxName() != null : fStep.boundingBoxName() != null){
            out.putListBoundingBox(step != null ? step.boundingBoxName() : fStep.boundingBoxName(), p.getSecond());
        }

        return out;
    }

    protected Pair<List<Image>,List<BoundingBox>> cropGrid(Mat m, List<Point> pxPoints, double gx, double gy) {
        Point tl = pxPoints.get(0);
        Point tr = pxPoints.get(1);
        Point bl = pxPoints.get(2);
        Point br = pxPoints.get(3);


        List<Image> out = new ArrayList<>();
        List<BoundingBox> bbox = (step != null ? step.boundingBoxName() != null : fStep.boundingBoxName() != null) ? new ArrayList<>() : null;
        for( int i=0; i<gx; i++ ){

            //x1, x2, x3, x4, y1, y2, y3, y4 - these represent the corner dimensions of the current row
            // within the overall grid
            int bx1 = (int) fracBetween (i/gx, tl.x(), tr.x());
            int bx2 = (int) fracBetween((i+1)/gx, tl.x(), tr.x());
            int by1 = (int) fracBetween (i/gx, tl.y(), tr.y());
            int by2 = (int) fracBetween((i+1)/gx, tl.y(), tr.y());
            int bx3 = (int) fracBetween (i/gx, bl.x(), br.x());
            int bx4 = (int) fracBetween((i+1)/gx, bl.x(), br.x());
            int by3 = (int) fracBetween (i/gx, bl.y(), br.y());
            int by4 = (int) fracBetween((i+1)/gx, bl.y(), br.y());

            for( int j=0; j<gy; j++ ){
                //Now, we need to segment the row, to get the grid square
                double sg2 = 1.0 / gy;

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

                int minX = min(ax1, ax2, ax3, ax4);
                int maxX = max(ax1, ax2, ax3, ax4);
                int minY = min(ay1, ay2, ay3, ay4);
                int maxY = max(ay1, ay2, ay3, ay4);

                int w = maxX-minX;
                int h = maxY-minY;

                if((step != null && step.aspectRatio() != null) || (fStep != null && fStep.aspectRatio() != null)){
                    double currAr = w / (double)h;
                    double ar = step != null ? step.aspectRatio() : fStep.aspectRatio();
                    if(ar < currAr){
                        //Need to increase height dimension to give desired AR
                        int newH = (int) (w / ar);
                        minY -= (newH-h)/2;
                        h = newH;
                    } else if(ar > currAr){
                        //Need ot increase width dimension to give desired AR
                        int newW = (int) (h * ar);
                        minX -= (newW-w)/2;
                        w = newW;
                    }
                }

                //Make sure bounds are inside image. TODO handle this differently for aspect ratio preserving?
                if(minX < 0){
                    w += minX;
                    minX = 0;
                }
                if(minX + w > m.cols()){
                    w = m.cols() - minX;
                }
                if(minY < 0){
                    h += minY;
                    minY = 0;
                }
                if(minY + h > m.rows()){
                    h = m.rows() - minY;
                }


                Rect r = new Rect(minX, minY, w, h);
                Mat crop = m.apply(r).clone();
                out.add(Image.create(crop));

                if(bbox != null){
                    bbox.add(BoundingBox.createXY(minX / (double)m.cols(), (minX + w)/ (double)m.cols(), minY/ (double)m.rows(), (minY + h)/ (double)m.rows()));
                }
            }
        }

        return Pair.of(out, bbox);
    }

    private double fracBetween(double frac, double a, double b){
        return a + frac * (b-a);
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
}
