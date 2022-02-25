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

package ai.konduit.serving.data.image.step.grayscale;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.Mat;

import static org.bytedeco.opencv.global.opencv_imgproc.*;


@CanRun({ GrayScaleStep.class})
public class GrayScaleRunner implements PipelineStepRunner {

    protected final GrayScaleStep step;



    public GrayScaleRunner(@NonNull GrayScaleStep step){
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

        if (imgName == null) {
            String errMultipleKeys = "DrawGridStep points field name was not provided and could not be inferred: multiple List<Point> fields exist: %s and %s";
            String errNoKeys = "DrawGridStep points field name was not provided and could not be inferred: no List<Point> fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

        Image i = data.getImage(imgName);

        Mat m = i.getAs(Mat.class).clone();
        cvtColor(m,m, COLOR_BGR2GRAY);
        //after gray scaling, convert the image to 3 channels
        if(step.outputChannels() != 1)
            cvtColor(m,m, COLOR_GRAY2BGR);


        Data out = data.clone();
        Image outImg = Image.create(m);
        out.put(imgName, outImg);

        return out;
    }


}
