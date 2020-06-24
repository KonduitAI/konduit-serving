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

package ai.konduit.serving.data.image.step.crop;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.util.DataUtils;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.util.ArrayList;
import java.util.List;

@CanRun(ImageCropStep.class)
public class ImageCropRunner implements PipelineStepRunner {

    private  final ImageCropStep step;

    public ImageCropRunner(ImageCropStep step){
        this.step = step;
    }

    @Override
    public void close() {
        //No op
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public Data exec(Context ctx, Data data) {

        //First: get names
        String name = step.imageName();
        if(name == null || name.isEmpty()){
            String errMultipleKeys = "ImageCropStep: Image field name was not provided and could not be inferred: multiple Image or List<Image> fields exist: %s and %s";
            String errNoKeys = "ImageCropStep: Image field name was not provided and could not be inferred: no Image or List<Image> fields exist in input Data";
            name = DataUtils.inferField(data, ValueType.IMAGE, true, errMultipleKeys, errNoKeys);
        }

        if(!data.has(name)){
            throw new IllegalStateException("Input image name \"" + name + "\" (via ImageCropStep.imageName config) " +
                    "is not present in the input Data instance. Data keys: " + data.keys());
        }
        if(!(data.type(name) == ValueType.IMAGE || (data.type(name) == ValueType.LIST && data.listType(name) == ValueType.IMAGE))){
            String t = data.type(name) == ValueType.LIST ? "List<" + data.listType(name) + ">" : data.type(name).name();
            throw new IllegalStateException("Input image name \"" + name + "\" (via ImageCropStep.imageName config) " +
                    "is present but is not an Image or List<Image> type. Data[\"" + name + "\"].type == " + t);
        }

        Data out = data.clone();
        if(data.type(name) == ValueType.IMAGE){
            Image i = data.getImage(name);
            out.put(name, crop(data, i));
        } else {
            //Must be list
            List<Image> list = data.getListImage(name);
            List<Image> newList = new ArrayList<>(list.size());
            for(Image i : list){
                newList.add(crop(data, i));
            }
            out.putListImage(name, newList);
        }
        return out;
    }

    protected Image crop(Data d, Image in){

        double x1, y1, x2, y2;
        if(step.cropBox() != null){
            BoundingBox bb = step.cropBox();
            x1 = bb.x1();
            y1 = bb.y1();
            x2 = bb.x2();
            y2 = bb.y2();
        } else if(step.cropPoints() != null){
            List<Point> p = step.cropPoints();
            Preconditions.checkState(p.size() == 2, "Expected 2 points for ImageCropStep.cropPoints field, got %s", p);
            x1 = p.get(0).x();
            y1 = p.get(0).y();
            x2 = p.get(1).x();
            y2 = p.get(1).y();
        } else if(step.cropName() != null){
            String n = step.cropName();
            Preconditions.checkState(d.has(n), "ImageCropStep.cropName = \"%s\" but input data does not have any field by this name", n);
            if(d.type(n) != ValueType.BOUNDING_BOX && !(d.type(n) == ValueType.LIST && d.listType(n) == ValueType.POINT)){
                String typeName = d.type(n) != ValueType.LIST ? "" + d.type(n) : "List<" + d.listType(n) + ">";
                throw new IllegalStateException("ImageCropStep.cropName must specify a BoundingBox or List<Point> field in the input Data instance, " +
                        "but Data[\"" + n + "\"] has type " + typeName);
            }

            if(d.type(n) == ValueType.BOUNDING_BOX){
                BoundingBox bb = d.getBoundingBox(n);
                x1 = bb.x1();
                y1 = bb.y1();
                x2 = bb.x2();
                y2 = bb.y2();
            } else {
                List<Point> p = d.getListPoint(n);
                Preconditions.checkState(p.size() == 2, "Expected 2 points for Data[ImageCropStep.cropName] field, got %s", p);
                x1 = p.get(0).x();
                y1 = p.get(0).y();
                x2 = p.get(1).x();
                y2 = p.get(1).y();
            }
        } else {
            throw new IllegalStateException("Error in ImageCropStep: one of cropPoints, cropBox or cropName must be set, but all are null");
        }

        if(!step.coordsArePixels()){
            x1 *= in.width();
            x2 *= in.width();
            y1 *= in.height();
            y2 *= in.height();
        }

        int px1 = (int)Math.round(x1);
        int px2 = (int)Math.round(x2);
        int py1 = (int)Math.round(y1);
        int py2 = (int)Math.round(y2);

        Mat m = in.getAs(Mat.class);
        if(inBoundW(px1, in) && inBoundW(px2, in) && inBoundH(py1, in) && inBoundH(py2, in)){
            //Easy/normal case - crop within image
            Mat out = m.apply(new Rect(px1, py1, (px2-px1), (py2-py1)));
            return Image.create(out);
        } else {
            //Crop region is at least partially outside the input image region
            Mat out = new Mat(px2-px1, py2-py1, m.type());
            UByteIndexer u = out.createIndexer(!Loader.getPlatform().startsWith("android"));
            u.pointer().zero();

            if((inBoundW(px1, in) || inBoundW(px2, in)) && (inBoundH(py1, in) || inBoundH(py2, in))){
                //Part of the input image overlap with the output crop region
                int ix1 = Math.max(0, px1);
                int iy1 = Math.max(0, py1);
                int ix2 = Math.min(in.width()-1, px2);
                int iy2 = Math.min(in.height()-1, py2);
                Mat sub = m.apply(new Rect(ix1, iy1, (ix2-ix1), (iy2-iy1)));        //Subset of the input image

                //Now, need to work out the coordinates of the output image to copy it to
                int x1c = 0;
                int y1c = 0;
                if (px1 < 0) {
                    x1c = out.cols() - sub.cols();
                }
                if(py1 < 0){
                    y1c = out.rows() - sub.rows();
                }

                Mat outSub = out.apply(new Rect(x1c, y1c, sub.cols(), sub.rows()));
                sub.copyTo(outSub);
            }
            return Image.create(out);
        }
    }

    protected boolean inBoundW(int x, Image img){
        return x >= 0 && x < img.width();
    }

    protected boolean inBoundH(int y, Image img){
        return y >= 0 && y < img.height();
    }
}
