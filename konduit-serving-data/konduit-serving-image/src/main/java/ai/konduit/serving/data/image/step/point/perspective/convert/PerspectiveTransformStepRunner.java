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

@CanRun(PerspectiveTransformStep.class)
public class PerspectiveTransformStepRunner implements PipelineStepRunner {

    protected final PerspectiveTransformStep step;

    public PerspectiveTransformStepRunner(@NonNull PerspectiveTransformStep step){
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
        // Step 1: create transformation matrix
        Mat sourceMat = pointsToMat(source);
        Mat targetMat = pointsToMat(target);
        Mat transMat = opencv_imgproc.getPerspectiveTransform(sourceMat, targetMat);

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
            throw new IllegalStateException("No fields found where PerspectiveTransformStepRunner could be applied.");
        }

        List<String> outNames = step.outputNames();
        if(outNames == null || outNames.size() == 0){
            outNames = fields;
        }else if(outNames.size() != fields.size()){
            throw new IllegalStateException("You must provide only as many outputNames as there are fields to be transformed! outputNames.size = "+step.outputNames().size()+" fields.size = "+fields.size());
        }

        // Step 3: apply transformation matrix to fields appropriately
        val out = Data.empty();
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
                                data.getListPoint(key).stream().map(it -> transform(transMat, it)).collect(Collectors.toList())
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
                                data.getListBoundingBox(key).stream().map(it -> transform(transMat, it)).collect(Collectors.toList())
                        );
                        break;
                    default:
                        throw new IllegalStateException("Field "+key+" with data type "+keyType+" is not supported for perspective transform!");
                }

            }else{
                switch (keyType){
                    case POINT:
                        out.put(outKey, transform(transMat, data.getPoint(key)));
                        break;
                    case IMAGE:
                        out.put(outKey, transform(transMat, data.getImage(key)));
                        break;
                    case BOUNDING_BOX:
                        out.put(outKey, transform(transMat, data.getBoundingBox(key)));
                        break;
                    default:
                        throw new IllegalStateException("Field "+key+" with data type "+keyType+" is not supported for perspective transform!");
                }
            }
        }

        if(step.keepOtherFields()){
            for(String key : data.keys()){
                if(fields.contains(key)){
                    out.copyFrom(key, data);
                }
            }
        }
        return out;
    }

    private Point transform(Mat transform, Point it) {
        Mat dst = new Mat();
        Mat src = new Mat(1, 1, CvType.CV_64FC(it.dimensions()));
        DoubleIndexer idx = src.createIndexer();
        for (int i = 0; i < it.dimensions(); i++) {
            idx.put(1, i, it.get(i));
        }
        opencv_core.perspectiveTransform(src, dst, transform);

        idx = dst.createIndexer();
        double[] coords = new double[it.dimensions()];
        for (int i = 0; i < it.dimensions(); i++) {
            coords[i] = idx.getDouble(1, i);
        }

        return Point.create(coords, it.label(), it.probability());
    }

    private BoundingBox transform(Mat transform, BoundingBox it) {
        Point transformedCenter = transform(transform, Point.create(it.cx(), it.cy()));
        return BoundingBox.create(transformedCenter.x(), transformedCenter.y(), it.width(), it.height(), it.label(), it.probability());
    }

    private Image transform(Mat transform, Image it) {
        Mat dst = new Mat();
        Mat src = it.getAs(Mat.class);
        Size outputSize = calculateOutputSize(transform, src);
        opencv_imgproc.warpPerspective(src, dst, transform, outputSize);
        return Image.create(dst);
    }

    private Size calculateOutputSize(Mat transform, Mat image) {
        int width = image.arrayWidth();
        int height = image.arrayHeight();
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

        return new Size((int)Math.round(maxX - minX), (int)Math.round(maxY - minY));
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
