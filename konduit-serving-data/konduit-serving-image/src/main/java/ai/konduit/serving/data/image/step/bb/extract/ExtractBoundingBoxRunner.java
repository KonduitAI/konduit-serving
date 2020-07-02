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

package ai.konduit.serving.data.image.step.bb.extract;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ImageUtils;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import lombok.NonNull;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CanRun(ExtractBoundingBoxStep.class)
public class ExtractBoundingBoxRunner implements PipelineStepRunner {

    protected final ExtractBoundingBoxStep step;

    public ExtractBoundingBoxRunner(@NonNull ExtractBoundingBoxStep step){
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

        String imgName = step.imageName();      //TODO find if null
        String bboxName = step.bboxName();      //TODO find if null

        if(imgName == null){
            String errMultipleKeys = "Image field name was not provided and could not be inferred: multiple image fields exist: %s and %s";
            String errNoKeys = "Image field name was not provided and could not be inferred: no image fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

        if(bboxName == null){
            String errMultipleKeys = "Bounding box field name was not provided and could not be inferred: multiple BoundingBox (or List<BoundingBox>) fields exist: %s and %s";
            String errNoKeys = "Bounding box field name was not provided and could not be inferred: no BoundingBox (or List<BoundingBox>) fields exist";
            bboxName = DataUtils.inferField(data, ValueType.BOUNDING_BOX, true, errMultipleKeys, errNoKeys);
        }


        Image i = data.getImage(imgName);
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

        ImageToNDArrayConfig im2ndConf = step.imageToNDArrayConfig();

        Mat img = i.getAs(Mat.class);
        List<Image> out = new ArrayList<>();
        for(BoundingBox bb : list) {
            bb = ImageUtils.accountForCrop(i, bb, im2ndConf);

            BoundingBox bbPx = BoundingBox.create(bb.cx() * img.cols(), bb.cy() * img.rows(),
                    bb.height() * img.rows(), bb.width() * img.cols());

            if(step.aspectRatio() != null){
                double desiredAR = step.aspectRatio();
                double actualAR = bbPx.width() / bbPx.height();
                if(desiredAR < actualAR){
                    //Increase height dimension to give desired AR
                    double newH = bbPx.width() / desiredAR;
                    bbPx = BoundingBox.create(bbPx.cx(), bbPx.cy(), newH, bbPx.width());
                } else if(desiredAR > actualAR){
                    //Increase width dimension to give desired AR
                    double newW = bbPx.height() * desiredAR;
                    bbPx = BoundingBox.create(bbPx.cx(), bbPx.cy(), bbPx.height(), newW);
                }
            }

            double x1 = Math.min(bbPx.x1(), bbPx.x2());
            double y1 = Math.min(bbPx.y1(), bbPx.y2());

            int x = (int) Math.round(x1);
            int y = (int)Math.round(y1);
            int h = (int)Math.round(bbPx.height());
            int w = (int)Math.round(bbPx.width());

            Rect r = new Rect(x, y, w, h);
            Mat m = img.apply(r);

            if(step.resizeH() != null && step.resizeW() != null){
                int rH = step.resizeH();
                int rW = step.resizeW();

                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(rH, rW));
                m = resized;
            }

            out.add(Image.create(m));
        }

        String outName = step.outputName() == null ? imgName : step.outputName();

        //TODO keep all other values (optionally)
        Data d;
        if(singleValue){
            d = Data.singleton(outName, out.get(0));
        } else {
            d = Data.singletonList(outName, out, ValueType.IMAGE);
        }

        if(step.keepOtherFields()){
            for(String s : data.keys()){
                if(!imgName.equals(s) && !bboxName.equals(s)){
                    d.copyFrom(s, data);
                }
            }
        }
        return d;
    }
}
