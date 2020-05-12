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

package ai.konduit.serving.pipeline.impl.step.ml;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SSDToBoundingBoxRunner implements PipelineStepRunner {

    private SSDToBoundingBoxStep step;

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        double threshold = 0.5;

        String key = "detection_boxes";     //TODO
        String prob = "detection_scores";

        NDArray bND = data.getNDArray(key);
        NDArray pND = data.getNDArray(prob);

        float[][][] bArr = bND.getAs(float[][][].class);        //Batch, num, xy
        float[][] pArr = pND.getAs(float[][].class);            //Batch, num

        List<BoundingBox> l = new ArrayList<>();
        for(int i=0; i<bArr[0].length; i++ ){
            double y1 = bArr[0][i][0];
            double x1 = bArr[0][i][1];
            double y2 = bArr[0][i][2];
            double x2 = bArr[0][i][3];
            double p = pArr[0][i];
            if(p >= threshold) {
                l.add(BoundingBox.createXY(x1, x2, y1, y2, null, p));
            }
        }


        return Data.singletonList("out", l, ValueType.BOUNDING_BOX);
    }
}
