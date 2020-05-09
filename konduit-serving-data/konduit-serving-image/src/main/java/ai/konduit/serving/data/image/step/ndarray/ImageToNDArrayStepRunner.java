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

package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

public class ImageToNDArrayStepRunner implements PipelineStepRunner {

    protected final ImageToNDArrayStep step;

    public ImageToNDArrayStepRunner(@NonNull ImageToNDArrayStep step){
        this.step = step;
    }

    @Override
    public void close() {
        //No-op
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        /*
        Behaviour:
        (a) If keys are defined, convert only those
        (b) In no keys are defined, convert all images
         */

        List<String> toConvert = step.getKeys();
        List<String> outNames = step.getOutputNames();
        boolean inferOutNames = (outNames == null) || outNames.isEmpty();
        if(inferOutNames)
            outNames = new ArrayList<>();

        if(toConvert == null){
            toConvert = new ArrayList<>();
            for(String s : data.keys()){
                if(data.type(s) == ValueType.IMAGE){
                    toConvert.add(s);

                    if(inferOutNames)
                        outNames.add(s);
                }
            }
        }

        Preconditions.checkState(toConvert.size() == outNames.size(), "Got (or inferred) a difference number of input images key" +
                " vs. output names: inputToConvert=%s, outputNames=%s", toConvert, outNames);

        Data d = Data.empty();
        int idx = 0;
        for(String s : toConvert){
            Image i = data.getImage(s);
            NDArray array = ImageToNDArray.convert(i, step.getConfig());
            d.put(outNames.get(idx++), array);
        }

        if(step.isKeepOtherValues()) {
            for (String s : data.keys()){
                if(toConvert.contains(s))
                    continue;
                d.copyFrom(s, data);
            }
        }

        return d;
    }
}
