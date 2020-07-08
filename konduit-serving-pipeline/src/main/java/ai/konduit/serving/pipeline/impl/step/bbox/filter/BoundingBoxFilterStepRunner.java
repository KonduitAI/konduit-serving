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
package ai.konduit.serving.pipeline.impl.step.bbox.filter;

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import org.nd4j.shade.guava.base.Preconditions;

import java.util.List;
import java.util.stream.Collectors;

public class BoundingBoxFilterStepRunner implements PipelineStepRunner {

    protected final BoundingBoxFilterStep step;
    public BoundingBoxFilterStepRunner(BoundingBoxFilterStep step) {
        this.step = step;
        Preconditions.checkArgument(!this.step.classesToKeep.isEmpty(),"Seems you forget to set the classes to keep.");
    };

    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }


    @Override
    public Data exec(Context ctx, Data data) {
        String key = "detection_boxes";     //TODO
        String prob = "detection_scores";
        String labels = "detection_classes";

        String inputName = step.inputName();
        if(inputName == null){
            String err = "No input name was set in the BoundingBoxFilterStep configuration and input name could not be guessed based on type";
            DataUtils.inferField(data, ValueType.BOUNDING_BOX, true, err + " (multiple keys)", err + " (no List<BoundingBox> values)");
        }

        List<String> classesToKeep = step.classesToKeep;
        List<BoundingBox> boundingBoxes = data
                .getListBoundingBox(inputName)
                .stream()
                .filter(i -> classesToKeep.contains(i.label()))
                .collect(Collectors.toList());


        String outName = step.outputName();
        if (outName == null)
            outName = BoundingBoxFilterStep.DEFAULT_OUTPUT_NAME;

        Data d = Data.singletonList(outName, boundingBoxes, ValueType.BOUNDING_BOX);

        if (step.keepOtherValues()) {
            for (String s : data.keys()) {
                if (!key.equals(s) && !prob.equals(s) &&!labels.equals(s) && !inputName.equals(s)) {
                    d.copyFrom(s, data);
                }
            }
        }


        return d;
    }


}
