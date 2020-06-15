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

package ai.konduit.serving.data.image.step.facemask;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

@CanRun(DrawFaceMaskStep.class)
public class DrawFaceMaskStepRunner implements PipelineStepRunner {


    protected final DrawFaceMaskStep step;

    public DrawFaceMaskStepRunner(@NonNull DrawFaceMaskStep step) {
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

        INDArray landmarks = data.getNDArray(step.landmarkArray()).getAs(INDArray.class);

        // https://github.com/AIZOOTech/FaceMaskDetection/blob/master/tensorflow_infer.py#L53
        INDArray y_bboxes_output = landmarks.tensorAlongDimension(0,0,0);
        INDArray y_cls_output = landmarks.tensorAlongDimension(0,0,1);

        Image img = data.getImage(step.image());


        String outputName = step.outputName();
        if (outputName == null) {
            outputName = DrawFaceMaskStep.DEFAULT_OUTPUT_NAME;
        }

        return Data.singleton(step.image(), Image.create(img));

    }

}


