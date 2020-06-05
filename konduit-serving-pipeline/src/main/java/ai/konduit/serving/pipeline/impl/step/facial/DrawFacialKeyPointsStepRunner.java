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
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.CvType;

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


        Image image = data.getImage(step.image());
        long[] shape = new long[]{image.height(),image.width()};


        NDArray landmarkArr = data.getNDArray(step.landmarkArray());
        INDArray marks = getDetectedMarks(landmarkArr);



        boolean drawingOnImage;
        Mat drawOn;
        drawOn = new Mat((int) shape[0], (int) shape[1], CvType.CV_8UC3);
  







        String outputName = step.outputName();
        if(outputName == null)
            outputName = DrawFacialKeyPointsStep.DEFAULT_OUTPUT_NAME;

        return Data.singleton(outputName, Image.create(drawOn));


    }

    private INDArray getDetectedMarks(NDArray landmark) {
        INDArray landmarkArr = landmark.getAs(INDArray.class);
        return Nd4j.toFlattened(landmarkArr).reshape(-1, 2);


    }
}
