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

package ai.konduit.serving.data.image.step.resize;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.util.ArrayList;
import java.util.List;

@CanRun(ImageResizeStep.class)
public class ImageResizeRunner implements PipelineStepRunner {

    private  final ImageResizeStep step;

    public ImageResizeRunner(ImageResizeStep step){
        this.step = step;
        Preconditions.checkState(step.height() != null || step.width() != null, "Error in ImageResizeStep: " +
                "at least one of height or width (for output) must be set");
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
        List<String> names = step.inputNames();
        if(names == null || names.isEmpty()){
            names = new ArrayList<>();
            for(String s : data.keys()){
                if(data.type(s) == ValueType.IMAGE){
                    names.add(s);
                } else if(data.type(s) == ValueType.LIST && data.listType(s) == ValueType.IMAGE){
                    names.add(s);
                }
            }

            if(names.isEmpty()){
                throw new IllegalStateException("Error in ImageResizeStep execution: No configuration was provided for " +
                        "inputNames and input Data does not have any Image or List<Image> fields. Data keys: " + data.keys());
            }
        } else {
            //Check
            for(String s : names){
                if(!data.has(s)){
                    throw new IllegalStateException("Input image name \"" + s + "\" (via ImageResizeStep.inputNames config) " +
                            "is not present in the input Data instance. Data keys: " + data.keys());
                }
                if(!(data.type(s) == ValueType.IMAGE || (data.type(s) == ValueType.LIST && data.listType(s) == ValueType.IMAGE))){
                    String t = data.type(s) == ValueType.LIST ? "List<" + data.listType(s) + ">" : data.type(s).name();
                    throw new IllegalStateException("Input image name \"" + s + "\" (via ImageResizeStep.inputNames config) " +
                            "is present but is not an Image or List<Image> type. Data[\"" + s + "\"].type == " + t);
                }
            }
        }

        Data out = data.clone();
        for(String s : names){
            if(data.type(s) == ValueType.IMAGE){
                Image i = data.getImage(s);
                out.put(s, resize(i));
            } else {
                //Must be list
                List<Image> list = data.getListImage(s);
                List<Image> newList = new ArrayList<>(list.size());
                for(Image i : list){
                    newList.add(resize(i));
                }
                out.putListImage(s, newList);
            }
        }
        return out;
    }

    protected Image resize(Image in){

        if((step.height() == null) != (step.width() == null)){
            //Only one is specified - no need to worry about aspect ratio
            int h, w;
            double ar = in.width() / (double)in.height();
            if(step.height() == null){
                w = step.width();
                h = (int)Math.round(w / ar);
            } else {
                h = step.height();
                w = (int)Math.round(ar * h);
            }
            Mat m = in.getAs(Mat.class);
            Mat resized = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(w, h));
            return Image.create(resized);
        } else {
            //Both h/w are specified - need to check and maybe handle aspect ratio
            if(in.height() == step.height() && in.width() == step.width()){
                return in;
            }

            double arCurrent = in.width() / (double)in.height();
            double arOut = step.width() / (double)step.height();
            Mat m = in.getAs(Mat.class);
            if(arCurrent == arOut || step.aspectRatioHandling() == AspectRatioHandling.STRETCH || step.aspectRatioHandling() == null){
                //Aspect ratio OK - or just stretching
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(m, resized, new Size(step.width(), step.height()));
                return Image.create(resized);
            } else {
                if(step.aspectRatioHandling() == AspectRatioHandling.CENTER_CROP){
                    Pair<Mat, BoundingBox> p = ImageToNDArray.centerCrop(m, step.height(), step.width(), false);
                    Mat crop = p.getFirst();
                    if(crop.rows() == step.height() && crop.cols() == step.width()){
                        return Image.create(crop);
                    }
                    Mat resized = new Mat();
                    org.bytedeco.opencv.global.opencv_imgproc.resize(crop, resized, new Size(step.width(), step.height()));
                    return Image.create(resized);
                } else if(step.aspectRatioHandling() == AspectRatioHandling.PAD){
                    if(arCurrent > arOut){
                        //Pad height
                        int newH = (int)Math.round(in.width() / arOut);
                        Mat padded = new Mat(newH, in.width(), m.type());
                        UByteIndexer u = padded.createIndexer(!Loader.getPlatform().startsWith("android"));
                        u.pointer().zero();
                        int delta = newH - in.height();

                        Mat sub = padded.apply(new Rect(0,delta/2,in.width(),in.height()));
                        m.copyTo(sub);

                        Mat resized = new Mat();
                        org.bytedeco.opencv.global.opencv_imgproc.resize(padded, resized, new Size(step.width(), step.height()));
                        return Image.create(resized);
                    } else {
                        //Pad width
                        int newW = (int)Math.round(in.height() * arOut);
                        Mat padded = new Mat(in.height(), newW, m.type());
                        UByteIndexer u = padded.createIndexer(!Loader.getPlatform().startsWith("android"));
                        u.pointer().zero();
                        int delta = newW - in.width();

                        Mat sub = padded.apply(new Rect(delta/2,0, in.width(), in.height()));
                        m.copyTo(sub);

                        Mat resized = new Mat();
                        org.bytedeco.opencv.global.opencv_imgproc.resize(padded, resized, new Size(step.height(), step.width()));
                        return Image.create(resized);
                    }
                } else {
                    throw new IllegalStateException("Unknown or not supported aspect ratio handling: " + step.aspectRatioHandling());
                }
            }
        }
    }
}
