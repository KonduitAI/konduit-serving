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

package ai.konduit.serving.data.image.step.point.perspective.convert;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.opencv.core.CvType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@Slf4j
@CanRun(PerspectiveTransformStep.class)
public class PerspectiveTransformRunner implements PipelineStepRunner {

    protected final PerspectiveTransformStep step;

    public PerspectiveTransformRunner(@NonNull PerspectiveTransformStep step){
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
        // Step 0: Get source and target points
        List<Point> source = null;
        if(step.sourcePoints() != null && step.sourcePointsName() != null){
            throw new IllegalStateException("You must not define both sourcePoints and sourcePointsName simultaneously on PerspectiveTransformStep!");
        }
        if(step.sourcePoints() == null && step.sourcePointsName() == null){
            throw new IllegalStateException("You have to define either sourcePoints or sourcePointsName on PerspectiveTransformStep!");
        }
        if(step.sourcePoints() != null){
            source = step.sourcePoints();
        }else{
            ValueType sourceType = data.type(step.sourcePointsName());
            if(sourceType == ValueType.LIST && data.listType(step.sourcePointsName()) == ValueType.POINT){
                List<Point> points = data.getListPoint(step.sourcePointsName());
                if(points.size() != 4){
                    throw new IllegalArgumentException("field "+step.sourcePointsName()+" for source points in PerspectiveTransformStep does not contain exactly 4 points (found: "+points.size()+")");
                }
                source = points;
            }
        }

        List<Point> target = null;
        if(step.targetPoints() != null && step.targetPointsName() != null){
            throw new IllegalStateException("You must not define both targetPoints and targetPointsName simultaneously on PerspectiveTransformStep!");
        }
        if(step.targetPoints() == null && step.targetPointsName() == null){
            target = calculateTargetPoints(source);
        } else if( step.targetPoints() != null){
            target = step.targetPoints();
        }else{
            ValueType targetType = data.type(step.targetPointsName());
            if(targetType == ValueType.LIST && data.listType(step.targetPointsName()) == ValueType.POINT){
                List<Point> points = data.getListPoint(step.targetPointsName());
                if(points.size() != 4){
                    throw new IllegalArgumentException("field "+step.targetPointsName()+" for target points in PerspectiveTransformStep does not contain exactly 4 points (found: "+points.size()+")");
                }
                source = points;
            }
        }

        int refWidth = -1;
        int refHeight = -1;
        if(step.referenceImage() != null){
            ValueType type = data.type(step.referenceImage());
            Image refImg;
            if(type == ValueType.IMAGE){
                refImg = data.getImage(step.referenceImage());
            }else if(type == ValueType.LIST && data.listType(step.referenceImage()) == ValueType.IMAGE){
                List<Image> images = data.getListImage(step.referenceImage());
                if(images.size() == 0){
                    throw new IllegalArgumentException("fild "+step.referenceImage()+" is an empty list");
                }
                refImg = images.get(0);
            }else{
                throw new IllegalArgumentException("field "+step.referenceImage()+" is neither an image nor a list of images");
            }
            refWidth = refImg.width();
            refHeight = refImg.height();
        }else if(step.referenceWidth() != null && step.referenceHeight() != null){
            refWidth = step.referenceWidth();
            refHeight = step.referenceHeight();
        }

        // Step 1: create transformation matrix
        Mat sourceMat = pointsToMat(source);
        Mat targetMat = pointsToMat(target);
        Mat transMat = getPerspectiveTransform(sourceMat, targetMat, refWidth, refHeight);

        // Step 2: find fields to apply transformation to
        List<String> fields = step.inputNames();
        if(fields == null){
            fields = new LinkedList<>();
            for (String key : data.keys()) {
                // Skip points that are used to define the transform
                if(key.equals(step.targetPointsName()) || key.equals(step.sourcePointsName())){ continue; }

                ValueType keyType = data.type(key);
                if(keyType == ValueType.LIST){
                    keyType = data.listType(key);
                }
                if(keyType == ValueType.IMAGE || keyType == ValueType.BOUNDING_BOX || keyType == ValueType.POINT){
                    fields.add(key);
                }
            }
        }
        if(fields.size() == 0){
            throw new IllegalStateException("No fields found where PerspectiveTransformRunner could be applied.");
        }

        List<String> outNames = step.outputNames();
        if(outNames == null || outNames.size() == 0){
            outNames = fields;
        }else if(outNames.size() != fields.size()){
            throw new IllegalStateException("You must provide only as many outputNames as there are fields to be transformed! outputNames.size = "+step.outputNames().size()+" fields.size = "+fields.size());
        }

        // Step 3: apply transformation matrix to fields appropriately
        val out = Data.empty();
        if(step.keepOtherFields()){
            for(String key : data.keys()){
                out.copyFrom(key, data);
            }
        }

        int rW = refWidth;
        int rH = refHeight;
        for (int i = 0; i < fields.size(); i++) {
            String key = fields.get(i);
            ValueType keyType = data.type(key);

            String outKey = outNames.get(i);

            if(keyType == ValueType.LIST){
                keyType = data.listType(key);
                switch (keyType){
                    case POINT:
                        out.putListPoint(
                                outKey,
                                data.getListPoint(key).stream().map(it -> transform(transMat, it, rW, rH)).collect(Collectors.toList())
                        );
                        break;
                    case IMAGE:
                        out.putListImage(
                                outKey,
                                data.getListImage(key).stream().map(it -> transform(transMat, it)).collect(Collectors.toList())
                        );
                        break;
                    case BOUNDING_BOX:
                        out.putListBoundingBox(
                                outKey,
                                data.getListBoundingBox(key).stream().map(it -> transform(transMat, it, rW, rH)).collect(Collectors.toList())
                        );
                        break;
                    default:
                        throw new IllegalStateException("Field "+key+" with data type "+keyType+" is not supported for perspective transform!");
                }

            }else{
                switch (keyType){
                    case POINT:
                        out.put(outKey, transform(transMat, data.getPoint(key), rW, rH));
                        break;
                    case IMAGE:
                        out.put(outKey, transform(transMat, data.getImage(key)));
                        break;
                    case BOUNDING_BOX:
                        out.put(outKey, transform(transMat, data.getBoundingBox(key), rW, rH));
                        break;
                    default:
                        throw new IllegalStateException("Field "+key+" with data type "+keyType+" is not supported for perspective transform!");
                }
            }
        }

        return out;
    }

    private Point transform(Mat transform, Point it, int refW, int refH) {
        it = it.toAbsolute(refW, refH);
        Mat dst = new Mat();
        Mat src = new Mat(1, 1, CvType.CV_64FC(it.dimensions()));
        DoubleIndexer idx = src.createIndexer();
        for (int i = 0; i < it.dimensions(); i++) {
            idx.put(0, i, it.get(i));
        }
        opencv_core.perspectiveTransform(src, dst, transform);

        idx = dst.createIndexer();
        double[] coords = new double[it.dimensions()];
        idx.get(0L, coords);

        return Point.create(coords, it.label(), it.probability());
    }

    private BoundingBox transform(Mat transform, BoundingBox it, int refW, int refH) {
        Point transformedCenter = transform(transform, Point.create(it.cx(), it.cy()), refW, refH);
        return BoundingBox.create(transformedCenter.x(), transformedCenter.y(), it.width(), it.height(), it.label(), it.probability());
    }

    private Image transform(Mat transform, Image it) {
        Mat dst = new Mat();
        Mat src = it.getAs(Mat.class);
        Size outputSize = calculateOutputSize(transform, it.width(), it.height());
        opencv_imgproc.warpPerspective(src, dst, transform, outputSize);
        return Image.create(dst);
    }

    private Mat getPerspectiveTransform(Mat sourceMat, Mat targetMat, int refWidth, int refHeight) {
        Mat initialTransform = opencv_imgproc.getPerspectiveTransform(sourceMat, targetMat);
        if(refWidth == -1 || refHeight == -1) { return initialTransform; }

        // Calculate where edges will end up in this case
        double[] extremes = calculateExtremes(initialTransform, refWidth, refHeight);

        FloatIndexer tIdx = targetMat.createIndexer();
        long rows = tIdx.size(0);
        for (long i = 0; i < rows; i++) {
            tIdx.put(i, 0, (float)(tIdx.get(i, 0) - extremes[0]));
            tIdx.put(i, 1, (float)(tIdx.get(i, 1) - extremes[1]));
        }

        return opencv_imgproc.getPerspectiveTransform(sourceMat, targetMat);
    }

    private double[] calculateExtremes(Mat transform, int width, int height) {
        Mat src = new Mat(4, 1, CvType.CV_64FC2);
        DoubleIndexer idx = src.createIndexer();
        // topLeft
        idx.put(0, 0, 0);
        idx.put(0, 1, 0);

        // topRight
        idx.put(1, 0, width);
        idx.put(1, 1, 0);

        // bottomLeft
        idx.put(2, 0, 0);
        idx.put(2, 1, height);

        // bottomRight
        idx.put(3, 0, width);
        idx.put(3, 1, height);


        Mat dst = new Mat();
        opencv_core.perspectiveTransform(src, dst, transform);

        idx = dst.createIndexer();
        double[] xValues = new double[]{
                idx.get(0, 0),
                idx.get(1, 0),
                idx.get(2, 0),
                idx.get(3, 0)
        };
        double[] yValues = new double[]{
                idx.get(0, 1),
                idx.get(1, 1),
                idx.get(2, 1),
                idx.get(3, 1)
        };

        double minX = DoubleStream.of(xValues).min().getAsDouble();
        double maxX = DoubleStream.of(xValues).max().getAsDouble();
        double minY = DoubleStream.of(yValues).min().getAsDouble();
        double maxY = DoubleStream.of(yValues).max().getAsDouble();

        return new double[]{minX, minY, maxX, maxY};
    }

    private Size calculateOutputSize(Mat transform, int width, int height) {
        double[] extremes = calculateExtremes(transform, width, height);
        double minX = extremes[0];
        double minY = extremes[1];
        double maxX = extremes[2];
        double maxY = extremes[3];

        int outputWidth = (int) Math.round(maxX - minX);
        int outputHeight = (int) Math.round(maxY - minY);
        if(outputWidth > 4096 || outputHeight > 4096){
            log.warn("Selected transform would create a too large output image ({}, {}})", outputWidth, outputHeight);
            outputWidth = Math.min(outputWidth, 4096);
            outputHeight = Math.min(outputHeight, 4096);
        }
        return new Size(outputWidth, outputHeight);
    }

    private List<Point> calculateTargetPoints(List<Point> source) {
        Point topLeft = source.get(0);
        Point topRight = source.get(1);
        Point bottomLeft = source.get(2);
        Point bottomRight = source.get(3);

        double width = Math.max(
                Math.sqrt(Math.pow(topLeft.x() - bottomLeft.x(), 2) + Math.pow(topLeft.y() - bottomLeft.y(), 2)),
                Math.sqrt(Math.pow(topRight.x() - bottomRight.x(), 2) + Math.pow(topRight.y() - bottomRight.y(), 2))
        );
        double height = Math.max(
                Math.sqrt(Math.pow(topLeft.x() - topRight.x(), 2) + Math.pow(topLeft.y() - topRight.y(), 2)),
                Math.sqrt(Math.pow(bottomLeft.x() - bottomRight.x(), 2) + Math.pow(bottomLeft.y() - bottomRight.y(), 2))
        );

        double originX = topLeft.x() <= width / 2 ? topLeft.x() : width - topLeft.x();
        double originY = topLeft.y() <= height / 2 ? topLeft.y() : height - topLeft.y();

        return Arrays.asList(
                Point.create(originX, originY),
                Point.create(originX + width, originY),
                Point.create(originX, originY + height),
                Point.create(originX + width, originY + height)
        );
    }

    private Mat pointsToMat(List<Point> points){
        int rows = points.size();
        int cols = points.get(1).dimensions();
        Mat mat = new Mat(rows, cols, CvType.CV_32F);
        FloatIndexer idx = mat.createIndexer();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                idx.put(i, j, (float)points.get(i).get(j));
            }
        }
        return mat;
    }
}
