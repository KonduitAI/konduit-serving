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

package ai.konduit.serving.data.image.step.facial;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;

import static ai.konduit.serving.data.image.step.facial.CropUtil.accountForCrop;
import static ai.konduit.serving.data.image.step.facial.CropUtil.scaleIfRequired;

public class DrawFacialKeyPointsStepRunner implements PipelineStepRunner {


    protected final DrawFacialKeyPointsStep step;

    public DrawFacialKeyPointsStepRunner(@NonNull DrawFacialKeyPointsStep step) {
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


        Image i = data.getImage(step.image());

        Mat m = i.getAs(Mat.class);

        INDArray landmarkArr = getDetectedMarks(data.getNDArray(step.landmarkArray()));
        List<BoundingBox> faces_bboxes = data.getListBoundingBox("img_bbox");
        Mat scaled = scaleIfRequired(m, this.step);

        ImageToNDArrayConfig im2ndConf = step.imageToNDArrayConfig();


        if (!faces_bboxes.isEmpty()) {
            for (BoundingBox face_bbox : faces_bboxes) {
                BoundingBox bb = accountForCrop(i, face_bbox, im2ndConf);

                double x1 = Math.min(bb.x1(), bb.x2());
                double y1 = Math.min(bb.y1(), bb.y2());

                int x = (int) (x1 * scaled.cols());
                int y = (int) (y1 * scaled.rows());
                int h = (int) Math.round(bb.height() * scaled.rows());
                int w = (int) Math.round(bb.width() * scaled.cols());
                Rect r = new Rect(x, y, w, h);
                org.bytedeco.opencv.global.opencv_imgproc.rectangle(scaled, r, Scalar.GREEN, 2, 8, 0);


                landmarkArr = landmarkArr.mul(bb.x2() - bb.x1());
                float[][] marks = landmarkArr.toFloatMatrix();
                for (int row = 0; row < marks.length; row++) {
                    Point point = new Point((int) ((landmarkArr.getRow(row).toFloatVector()[0] + bb.x1()) * scaled.cols()), (int) ((landmarkArr.getRow(row).toFloatVector()[1] + bb.y1()) * scaled.rows()));

                    org.bytedeco.opencv.global.opencv_imgproc.circle(scaled, point, 1, Scalar.RED);
                }

            }


        }
        String outputName = step.outputName();
        if (outputName == null) {
            outputName = DrawFacialKeyPointsStep.DEFAULT_OUTPUT_NAME;
        }

        return Data.singleton(step.image(), Image.create(scaled));

    }


    private INDArray getDetectedMarks(NDArray landmark) {
        INDArray landmarkArr = landmark.getAs(INDArray.class);
        return Nd4j.toFlattened(landmarkArr).reshape(-1, 2);

    }


}


