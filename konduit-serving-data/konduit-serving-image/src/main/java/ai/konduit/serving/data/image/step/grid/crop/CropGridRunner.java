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
public class CropGridRunner implements PipelineStepRunner {

    protected final CropGridStep step;
    protected final CropFixedGridStep fStep;

    public CropGridRunner(@NonNull CropGridStep step) {
        this.step = step;
        this.fStep = null;
    }

    public CropGridRunner(@NonNull CropFixedGridStep step) {
        this.step = null;
        this.fStep = step;
    }

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        if (step != null)
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

        if (fixed) {
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
        if (isPx) {
            pxPoints = points;
        } else {
            pxPoints = new ArrayList<>(4);
            for (Point p : points) {
                pxPoints.add(Point.create(p.x() * i.width(), p.y() * i.height()));
            }
        }


        Mat m = i.getAs(Mat.class);
        double gx = fixed ? fStep.gridX() : step.gridX();
        double gy = fixed ? fStep.gridY() : step.gridY();
        Pair<List<Image>, List<BoundingBox>> p = cropGrid(m, pxPoints, gx, gy);

        Data out;
        if (step != null ? step.keepOtherFields() : fStep.keepOtherFields()) {
            out = data.clone();
        } else {
            out = Data.empty();
        }

        String outName = (step != null ? step.outputName() : fStep.outputName());
        if (outName == null)
            outName = CropGridStep.DEFAULT_OUTPUT_NAME;
        out.putListImage(outName, p.getFirst());

        if (step != null ? step.boundingBoxName() != null : fStep.boundingBoxName() != null) {
            out.putListBoundingBox(step != null ? step.boundingBoxName() : fStep.boundingBoxName(), p.getSecond());
        }

        return out;
    }

    protected Pair<List<Image>, List<BoundingBox>> cropGrid(Mat m, List<Point> pxPoints, double gx, double gy) {
        Point tl = pxPoints.get(0);
        Point tr = pxPoints.get(1);
        Point bl = pxPoints.get(2);
        Point br = pxPoints.get(3);


        List<Image> out = new ArrayList<>();
        List<BoundingBox> bbox = (step != null ? step.boundingBoxName() != null : fStep.boundingBoxName() != null) ? new ArrayList<>() : null;
        //Note we are iterating (adding to output) in order: (0,0), (0, 1), ..., (0, C-1), ..., (R-1, C-1) - i.e., per row
        for (int j = 0; j < gy; j++) {
            for (int i = 0; i < gx; i++) {
                //Work out the corners of the current crop box
                Point boxTL = topLeft(j, i, (int) gy, (int) gx, tl, tr, bl, br);
                Point boxTR = topRight(j, i, (int) gy, (int) gx, tl, tr, bl, br);
                Point boxBL = bottomLeft(j, i, (int) gy, (int) gx, tl, tr, bl, br);
                Point boxBR = bottomRight(j, i, (int) gy, (int) gx, tl, tr, bl, br);

                double minX = min(boxTL.x(), boxTR.x(), boxBL.x(), boxBR.x());
                double maxX = max(boxTL.x(), boxTR.x(), boxBL.x(), boxBR.x());
                double minY = min(boxTL.y(), boxTR.y(), boxBL.y(), boxBR.y());
                double maxY = max(boxTL.y(), boxTR.y(), boxBL.y(), boxBR.y());

                int w = (int)(maxX - minX);
                int h = (int)(maxY - minY);

                if ((step != null && step.aspectRatio() != null) || (fStep != null && fStep.aspectRatio() != null)) {
                    double currAr = w / (double) h;
                    double ar = step != null ? step.aspectRatio() : fStep.aspectRatio();
                    if (ar < currAr) {
                        //Need to increase height dimension to give desired AR
                        int newH = (int) (w / ar);
                        minY -= (newH - h) / 2.0;
                        h = newH;
                    } else if (ar > currAr) {
                        //Need ot increase width dimension to give desired AR
                        int newW = (int) (h * ar);
                        minX -= (newW - w) / 2.0;
                        w = newW;
                    }
                }

                //Make sure bounds are inside image. TODO handle this differently for aspect ratio preserving?
                if (minX < 0) {
                    w += minX;
                    minX = 0;
                }
                if (minX + w > m.cols()) {
                    w = m.cols() - (int)minX;
                }
                if (minY < 0) {
                    h += minY;
                    minY = 0;
                }
                if (minY + h > m.rows()) {
                    h = m.rows() - (int)minY;
                }


                Rect r = new Rect((int)minX, (int)minY, w, h);
                Mat crop = m.apply(r).clone();
                out.add(Image.create(crop));

                if (bbox != null) {
                    bbox.add(BoundingBox.createXY(minX / (double) m.cols(), (minX + w) / (double) m.cols(), minY / (double) m.rows(), (minY + h) / (double) m.rows()));
                }
            }
        }

        return Pair.of(out, bbox);
    }

    private Point topLeft(int row, int col, int numRows, int numCols, Point tl, Point tr, Point bl, Point br) {
        //Here, we are stepping "rows/numRows" between TL/BL and TR/BR
        //This gives us the line along which the
        //Then we just need to step between those points
        /*
        i.e., for O=(1,2) we work out (x,y) for A and B, then step 2/numCols from A to B
        |-----------------|
        |     |     |     |
        A-----|-----O-----B
        |     |     |     |
        |-----------------|
         */
        Point tlbl = fracBetween(row / (double) numRows, tl, bl);
        Point trbr = fracBetween(row / (double) numRows, tr, br);

        return fracBetween(col / (double) numCols, tlbl, trbr);
    }

    private Point bottomRight(int row, int col, int numRows, int numCols, Point tl, Point tr, Point bl, Point br) {
        return topLeft(row + 1, col + 1, numRows, numCols, tl, tr, bl, br);
    }

    private Point bottomLeft(int row, int col, int numRows, int numCols, Point tl, Point tr, Point bl, Point br) {
        return topLeft(row + 1, col, numRows, numCols, tl, tr, bl, br);
    }

    private Point topRight(int row, int col, int numRows, int numCols, Point tl, Point tr, Point bl, Point br) {
        return topLeft(row, col + 1, numRows, numCols, tl, tr, bl, br);
    }

    Point fracBetween(double frac, Point p1, Point p2) {
        return Point.create(fracBetween(frac, p1.x(), p2.x()), fracBetween(frac, p1.y(), p2.y()));
    }

    private double fracBetween(double frac, double a, double b) {
        return a + frac * (b - a);
    }

    private double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }
}
