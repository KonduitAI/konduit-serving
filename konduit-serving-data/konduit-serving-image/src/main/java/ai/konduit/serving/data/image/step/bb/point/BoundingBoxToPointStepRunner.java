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

package ai.konduit.serving.data.image.step.bb.point;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CanRun(BoundingBoxToPointStep.class)
public class BoundingBoxToPointStepRunner implements PipelineStepRunner {

    protected final BoundingBoxToPointStep step;

    public BoundingBoxToPointStepRunner(@NonNull BoundingBoxToPointStep step){
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
        String bboxName = step.bboxName();

        if(bboxName == null){
            String errMultipleKeys = "Bounding box field name was not provided and could not be inferred: multiple BoundingBox (or List<BoundingBox>) fields exist: %s and %s";
            String errNoKeys = "Bounding box field name was not provided and could not be inferred: no BoundingBox (or List<BoundingBox>) fields exist";
            bboxName = DataUtils.inferField(data, ValueType.BOUNDING_BOX, true, errMultipleKeys, errNoKeys);
        }

        ValueType vt = data.type(bboxName);

        List<BoundingBox> list;

        boolean singleValue;
        if(vt == ValueType.BOUNDING_BOX){
            list = Collections.singletonList(data.getBoundingBox(bboxName));
            singleValue = true;
        } else if(vt == ValueType.LIST){
            if(data.listType(bboxName) == ValueType.BOUNDING_BOX) {
                list = data.getListBoundingBox(bboxName);
            } else {
                throw new IllegalStateException("Data[" + bboxName + "] is List<" + data.listType(bboxName) + "> not List<BoundingBox>");
            }
            singleValue = false;
        } else {
            throw new IllegalStateException("Data[" + bboxName + "] is neither a BoundingBox or List<BoundingBox> - is " + vt);
        }

        List<Point> out = new ArrayList<>();
        for(BoundingBox bb : list) {
            switch (step.method()){
                case TOP_LEFT:
                    out.add(Point.create(bb.x1(), bb.y1(), bb.label(), bb.probability()));
                    break;
                case TOP_RIGHT:
                    out.add(Point.create(bb.x2(), bb.y1(), bb.label(), bb.probability()));
                    break;
                case BOTTOM_LEFT:
                    out.add(Point.create(bb.x1(), bb.y2(), bb.label(), bb.probability()));
                    break;
                case BOTTOM_RIGHT:
                    out.add(Point.create(bb.x2(), bb.y2(), bb.label(), bb.probability()));
                    break;
                case CENTER:
                    out.add(Point.create(bb.cx(), bb.cy(), bb.label(), bb.probability()));
                    break;
            }
        }

        String outName = step.outputName() == null ? bboxName : step.outputName();

        Data d;
        if(singleValue){
            d = Data.singleton(outName, out.get(0));
        } else {
            d = Data.singletonList(outName, out, ValueType.POINT);
        }

        if(step.keepOtherFields()){
            for(String s : data.keys()){
                if(!bboxName.equals(s)){
                    d.copyFrom(s, data);
                }
            }
        }
        return d;
    }
}
