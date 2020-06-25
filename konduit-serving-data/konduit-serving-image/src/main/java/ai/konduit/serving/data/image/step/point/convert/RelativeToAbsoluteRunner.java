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

package ai.konduit.serving.data.image.step.point.convert;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ImageUtils;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

@CanRun(RelativeToAbsoluteStep.class)
public class RelativeToAbsoluteRunner implements PipelineStepRunner {

    private final RelativeToAbsoluteStep step;

    public RelativeToAbsoluteRunner(RelativeToAbsoluteStep step){
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

        List<String> toConvert = step.toConvert();
        Preconditions.checkState(toConvert != null, "No Data field names were set for conversion via RelativeToAbsolute.toConvert");

        //Work out image dims
        int h;
        int w;
        if(step.imageName() != null){
            Preconditions.checkState(data.has(step.imageName()), "RelativeToAbsoluteStep.imageName=\"%s\" but Data has no field with this name", step.imageName());
            Preconditions.checkState(data.type(step.imageName()) == ValueType.IMAGE, "RelativeToAbsoluteStep.imageName=\"%s\" but Data[\"%s\" has type %s",
                    step.imageName(), step.imageName(), data.type(step.imageName()));
            Image i = data.getImage(step.imageName());
            h = i.height();
            w = i.width();
        } else if(step.imageH() != null && step.imageW() != null){
            h = step.imageH();
            w = step.imageW();
        } else {
            throw new IllegalStateException("RelativeToAbsoluteStep: Either imageH and imageW must be set, or imageName must be set, " +
                    "in order to determine the image size");
        }

        Data out = data.clone();
        for(String s : toConvert){
            Preconditions.checkState(data.has(s), "Error in RelativeToAbsoluteStep: data does not have an input of name \"%s\"", s);
            ValueType vt = data.type(s);
            boolean list = vt == ValueType.LIST;

            if(vt != ValueType.POINT && vt != ValueType.BOUNDING_BOX && (!list || !(data.listType(s) == ValueType.POINT || data.listType(s) == ValueType.BOUNDING_BOX))){
                String type = list ? "List<" + data.listType(s) + ">" : vt.toString();
                throw new IllegalStateException("Error in RelativeToAbsoluteStep: Value for input \"" + s + "\" must be POINT, BOUNDING_BOX, " +
                        "LIST<POINT> or LIST<BOUNDING_BOX> but was " + type);
            }

            if(vt == ValueType.POINT){
                Point p = data.getPoint(s);
                p = ImageUtils.accountForCrop(p, w, h, step.imageToNDArrayConfig());
                out.put(s, p);
            } else if(vt == ValueType.BOUNDING_BOX){
                BoundingBox bb = data.getBoundingBox(s);
                bb = ImageUtils.accountForCrop(bb, w, h, step.imageToNDArrayConfig());
                out.put(s, bb);
            } else if(data.listType(s) == ValueType.POINT){
                List<Point> lIn = data.getListPoint(s);
                List<Point> lOut = new ArrayList<>();
                for(Point p : lIn){
                    lOut.add(ImageUtils.accountForCrop(p, w, h, step.imageToNDArrayConfig()));
                }
                out.putListPoint(s, lOut);
            } else if(data.listType(s) == ValueType.BOUNDING_BOX){
                List<BoundingBox> lIn = data.getListBoundingBox(s);
                List<BoundingBox> lOut = new ArrayList<>();
                for(BoundingBox bb : lIn){
                    lOut.add(ImageUtils.accountForCrop(bb, w, h, step.imageToNDArrayConfig()));
                }
                out.putListBoundingBox(s, lOut);
            } else {
                throw new RuntimeException();   //Should never happen
            }
        }

        return out;
    }
}
