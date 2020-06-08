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

package ai.konduit.serving.pipeline.impl.step.facial;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.*;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.opencv.core.CvType;

import java.util.List;

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
        Mat resized = new Mat();
        org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(i.width(), i.height()));



        INDArray landmarkArr = getDetectedMarks(data.getNDArray(step.landmarkArray()));
        List<BoundingBox> faces_bboxes = data.getListBoundingBox("img_bbox");

            if (!faces_bboxes.isEmpty()) {
            for (BoundingBox face_bbox : faces_bboxes) {

                landmarkArr = landmarkArr.mul(face_bbox.x2() - face_bbox.x1());

//                landmarkArr = landmarkArr.get(NDArrayIndex.all(), NDArrayIndex.point(0)).add(face_bbox.x1());
//                landmarkArr = landmarkArr.get(NDArrayIndex.all(), NDArrayIndex.point(1)).add(face_bbox.y1());

                float[][] marks = landmarkArr.toFloatMatrix();
                for (int row = 0; row < marks.length; row++) {
                Point point = new Point((int) (landmarkArr.getRow(row).toFloatVector()[0] * resized.rows()), (int) (landmarkArr.getRow(row).toFloatVector()[1]*resized.cols()));
                org.bytedeco.opencv.global.opencv_imgproc.circle(resized,point, 1, Scalar.RED);
                }

            }
            }




        String outputName = step.outputName();
        if (outputName == null)
            outputName = DrawFacialKeyPointsStep.DEFAULT_OUTPUT_NAME;


        return Data.singleton(outputName, Image.create(resized));


    }

    private INDArray getDetectedMarks(NDArray landmark) {
        INDArray landmarkArr = landmark.getAs(INDArray.class);
        return Nd4j.toFlattened(landmarkArr).reshape(-1, 2);

    }


}


