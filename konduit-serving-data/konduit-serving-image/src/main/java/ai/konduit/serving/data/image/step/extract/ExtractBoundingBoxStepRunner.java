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

package ai.konduit.serving.data.image.step.extract;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
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

public class ExtractBoundingBoxStepRunner implements PipelineStepRunner {

    protected final ExtractBoundingBoxStep step;

    public ExtractBoundingBoxStepRunner(@NonNull ExtractBoundingBoxStep step){
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
            bb = accountForCrop(i, bb, im2ndConf);

            if(step.aspectRatio() != null){
                double desiredAR = step.aspectRatio();
                double actualAR = bb.width() / bb.height();
                if(desiredAR < actualAR){
                    //Increase height dimension to give desired AR
                    double newH = bb.width() / desiredAR;
                    bb = BoundingBox.create(bb.cx(), bb.cy(), newH, bb.width());
                } else if(desiredAR > actualAR){
                    //Increase width dimension to give desired AR
                    double newW = bb.height() * desiredAR;
                    bb = BoundingBox.create(bb.cx(), bb.cy(), bb.height(), newW);
                }
            }

            double x1 = Math.min(bb.x1(), bb.x2());
            double y1 = Math.min(bb.y1(), bb.y2());

            int x = (int)(x1 * img.cols());
            int y = (int)(y1 * img.rows());
            int h = (int) Math.round(bb.height() * img.rows());
            int w = (int)Math.round(bb.width() * img.cols());
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

    protected BoundingBox accountForCrop(Image image, BoundingBox bbox, ImageToNDArrayConfig config){
        if(config == null)
            return bbox;

        BoundingBox cropRegion = ImageToNDArray.getCropRegion(image, config);

        double cropWidth = cropRegion.width();
        double cropHeight = cropRegion.height();

        double x1 = cropRegion.x1() + cropWidth * bbox.x1();
        double x2 = cropRegion.x1() + cropWidth * bbox.x2();
        double y1 = cropRegion.y1() + cropHeight * bbox.y1();
        double y2 = cropRegion.y1() + cropHeight * bbox.y2();

        return BoundingBox.createXY(x1, x2, y1, y2, bbox.label(), bbox.probability());
    }
}
