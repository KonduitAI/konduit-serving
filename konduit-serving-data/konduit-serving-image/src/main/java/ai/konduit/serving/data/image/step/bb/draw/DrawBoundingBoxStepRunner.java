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

package ai.konduit.serving.data.image.step.bb.draw;

import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.util.ColorUtil;
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
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DrawBoundingBoxStepRunner implements PipelineStepRunner {




    protected final DrawBoundingBoxStep step;

    public DrawBoundingBoxStepRunner(@NonNull DrawBoundingBoxStep step){
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
        String bboxName = step.bboxName();

        if(imgName == null){
            String errMultipleKeys = "Image field name was not provided and could not be inferred: multiple image fields exist: %s and %s";
            String errNoKeys = "Image field name was not provided and could not be inferred: no image fields exist";
            imgName = DataUtils.inferField(data, ValueType.IMAGE, false, errMultipleKeys, errNoKeys);
        }

        if(bboxName == null){
            String errMultipleKeys = "Bounding box field name was not provided and could not be inferred: multiple bounding box fields exist: %s and %s";
            String errNoKeys = "Bounding box field name was not provided and could not be inferred: no bounding box fields exist";
            bboxName = DataUtils.inferField(data, ValueType.BOUNDING_BOX, false, errMultipleKeys, errNoKeys);
        }


        Image i = data.getImage(imgName);
        ValueType vt = data.type(bboxName);

        List<BoundingBox> list;

        if(vt == ValueType.BOUNDING_BOX){
            list = Collections.singletonList(data.getBoundingBox(bboxName));
        } else if(vt == ValueType.LIST){
            if(data.listType(bboxName) == ValueType.BOUNDING_BOX) {
                list = data.getListBoundingBox(bboxName);
            } else {
                throw new IllegalStateException("Data[" + bboxName + "] is List<" + data.listType(bboxName) + "> not List<BoundingBox>");
            }
        } else {
            throw new IllegalStateException("Data[" + bboxName + "] is neither a BoundingBox or List<BoundingBox> - is " + vt);
        }

        Mat m = i.getAs(Mat.class);
        Map<String,String> cc = step.classColors();
        String dc = step.color();

        Mat scaled = scaleIfRequired(m);

        int thickness = Math.max(1, step.lineThickness());

        ImageToNDArrayConfig im2ndConf = step.imageToNDArrayConfig();

        for(BoundingBox bb : list) {
            bb = accountForCrop(i, bb, im2ndConf);

            Scalar color;
            if (step.classColors() == null && step.color() == null) {
                //No color specified - use default color
                color = Scalar.GREEN;
            } else {
                if (cc != null && bb.label() != null && cc.containsKey(bb.label())){
                    String s = cc.get(bb.label());
                    color = ColorUtil.stringToColor(s);
                } else if(dc != null){
                    color = ColorUtil.stringToColor(dc);
                } else {
                    color = Scalar.GREEN;
                }
            }

            double x1 = Math.min(bb.x1(), bb.x2());
            double y1 = Math.min(bb.y1(), bb.y2());

            int x = (int)(x1 * scaled.cols());
            int y = (int)(y1 * scaled.rows());
            int h = (int) Math.round(bb.height() * scaled.rows());
            int w = (int)Math.round(bb.width() * scaled.cols());
            Rect r = new Rect(x, y, w, h);
            org.bytedeco.opencv.global.opencv_imgproc.rectangle(scaled, r, color, thickness, 8, 0);
        }

        if(im2ndConf != null && step.drawCropRegion()){
            BoundingBox bb = ImageToNDArray.getCropRegion(i, im2ndConf);

            Scalar color;
            if (step.cropRegionColor() == null) {
                //No color specified - use default color
                color = Scalar.BLUE;
            } else {
                color = ColorUtil.stringToColor(step.cropRegionColor());
            }

            int x = (int)(bb.x1() * scaled.cols());
            int y = (int)(bb.y1() * scaled.rows());
            int h = (int)(bb.height() * scaled.rows());
            int w = (int)(bb.width() * scaled.cols());
            Rect r = new Rect(x, y, w, h);
            org.bytedeco.opencv.global.opencv_imgproc.rectangle(scaled, r, color, thickness, 8, 0);
        }

        return Data.singleton(imgName, Image.create(scaled));
    }

    protected Mat scaleIfRequired(Mat m){
        if(step.scale() != null && step.scale() != DrawBoundingBoxStep.Scale.NONE){
            boolean scaleRequired = false;
            int newH = 0;
            int newW = 0;
            if(step.scale() == DrawBoundingBoxStep.Scale.AT_LEAST){
                if(m.rows() < step.resizeH() || m.cols() < step.resizeW()){
                    scaleRequired = true;
                    double ar = m.cols() / (double)m.rows();
                    if(m.rows() < step.resizeH() && m.cols() >= step.resizeW()){
                        //Scale height
                        newW = step.resizeW();
                        newH = (int)(newW / ar);
                    } else if(m.rows() > step.resizeH() && m.cols() < step.resizeW()){
                        //Scale width
                        newH = step.resizeH();
                        newW = (int) (ar * newH);
                    } else {
                        //Scale both dims...
                        if((int)(step.resizeW() / ar) < step.resizeH()){
                            //Scale height
                            newW = step.resizeW();
                            newH = (int)(newW / ar);
                        } else {
                            //Scale width
                            newH = step.resizeH();
                            newW = (int) (ar * newH);
                        }
                    }
                }
            } else if(step.scale() == DrawBoundingBoxStep.Scale.AT_MOST){
                Preconditions.checkState(step.resizeH() > 0 && step.resizeW() > 0, "Invalid resize: resizeH=%s, resizeW=%s", step.resizeH(), step.resizeW());
                if(m.rows() > step.resizeH() || m.cols() > step.resizeW()){
                    scaleRequired = true;
                    double ar = m.cols() / (double)m.rows();
                    if(m.rows() > step.resizeH() && m.cols() <= step.resizeW()){
                        //Scale height
                        newW = step.resizeW();
                        newH = (int)(newW / ar);
                    } else if(m.rows() < step.resizeH() && m.cols() > step.resizeW()){
                        //Scale width
                        newH = step.resizeH();
                        newW = (int) (ar * newH);
                    } else {
                        //Scale both dims...
                        if((int)(step.resizeW() / ar) > step.resizeH()){
                            //Scale height
                            newW = step.resizeW();
                            newH = (int)(newW / ar);
                        } else {
                            //Scale width
                            newH = step.resizeH();
                            newW = (int) (ar * newH);
                        }
                    }
                }
            }

            if(scaleRequired){
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(newH, newW));
                return resized;
            } else {
                return m;
            }

        } else {
            return m;
        }
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
