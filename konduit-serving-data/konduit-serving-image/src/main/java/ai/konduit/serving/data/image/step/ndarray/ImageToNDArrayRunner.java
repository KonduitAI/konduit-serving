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

package ai.konduit.serving.data.image.step.ndarray;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.ValueNotFoundException;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import lombok.NonNull;
import org.bytedeco.javacpp.Loader;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CanRun(ImageToNDArrayStep.class)
public class ImageToNDArrayRunner implements PipelineStepRunner {

    protected final ImageToNDArrayStep step;

    public ImageToNDArrayRunner(@NonNull ImageToNDArrayStep step){
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

        List<String> toConvert = step.keys();
        List<String> outNames = step.outputNames();
        boolean inferOutNames = (outNames == null) || outNames.isEmpty();
        if(inferOutNames) {
            outNames = new ArrayList<>();
        }

        if(toConvert == null){
            toConvert = new ArrayList<>();
            for(String s : data.keys()){
                if(data.type(s) == ValueType.IMAGE){
                    toConvert.add(s);

                    if(inferOutNames)
                        outNames.add(s);
                } else if(step.config().listHandling() != ImageToNDArrayConfig.ListHandling.NONE && data.type(s) == ValueType.LIST && data.listType(s) == ValueType.IMAGE){
                    toConvert.add(s);

                    if(inferOutNames)
                        outNames.add(s);
                }
            }
        }

        Preconditions.checkState(!toConvert.isEmpty(), "No input images were specified, and no Image field could be inferred from input");

        Preconditions.checkState(toConvert.size() == outNames.size(), "Got (or inferred) a difference number of input images key" +
                " vs. output names: inputToConvert=%s, outputNames=%s", toConvert, outNames);

        boolean meta = step.metadata();
        List<BoundingBox> cropRegionMeta = meta ? new ArrayList<>(toConvert.size()) : null;
        List<Long> origHMeta = meta ? new ArrayList<>(toConvert.size()) : null;
        List<Long> origWMeta = meta ? new ArrayList<>(toConvert.size()) : null;

        Data d = Data.empty();
        int idx = 0;
        for(String s : toConvert){
            if(!data.has(s)) {
                throw new ValueNotFoundException("Error in ImageToNDArrayStep: Input field \"" + s + "\" (via ImageToNDArrayStep.keys configuration)" +
                        " does not exist in the provided input Data instance (data keys: " + data.keys() + ")");
            }

            boolean isList = data.type(s) == ValueType.LIST && data.listType(s) == ValueType.IMAGE;

            if(isList){
                List<NDArray> l = new ArrayList<>();
                boolean batch = false;
                switch (step.config().listHandling()){
                    default:
                    case NONE:
                        throw new IllegalStateException("Error in step " + name() + " of type ImageToNDArrayStep: input field \"" +
                                s + "\" is a List<Image> but ImageToNDArrayConfig.listHandling == ListHandling.NONE.\n" +
                                "For List<Image> --> List<NDArray>, use ListHandling.LIST_OUT\n" +
                                "For List<Image> --> NDArray, use ListHandling.BATCH (where arrays are batched along dimension 0)\n" +
                                "For List<Image>.get(0) --> NDArray, use ListHandling.FIRST");
                    case FIRST:
                        List<Image> imgList = data.getListImage(s);
                        if(imgList.isEmpty()){
                            empty(d, outNames.get(idx++));
                        } else {
                            NDArray array = ImageToNDArray.convert(data.getListImage(s).get(0), step.config());
                            d.put(outNames.get(idx++), array);
                        }
                        return d;
                    case BATCH:
                        batch = true;   //Fall through
                    case LIST_OUT:
                        List<Image> images = data.getListImage(s);
                        for(Image i : images){
                            NDArray out = ImageToNDArray.convert(i, step.config());
                            l.add(out);
                        }
                        break;
                }

                if(batch){
                    if(l.size() == 0) {
                        //Return empty NDArray
                        empty(d, outNames.get(idx++));
                        continue;
                    } else if(l.size() == 1){
                        d.put(outNames.get(idx++), l.get(0));
                    } else {
                        //Check that all have the same shape before combining
                        long[] first = l.get(0).shape();
                        for (NDArray arr : l) {
                            long[] curr = arr.shape();
                            Preconditions.checkState(Arrays.equals(first, curr), "Error in ImageToNDArrayStep:" +
                                    "ImageToNDArrayStep.config.listHandling == BATCH but at least two output NDArrays have different shapes" +
                                    "(%s vs. %s). Unable to combine multiple NDArrays according to batch dimension if they have different shapes." +
                                    " Setting ImageToNDArrayStep.config.height/width or only passing in all the same size images will solve this problem", first, curr);
                        }

                        //Concatenate. Note that C order along dimension 0 means we can just copy buffers
                        SerializedNDArray nd = l.get(0).getAs(SerializedNDArray.class);
                        int size = nd.getBuffer().capacity();
                        int newSize = size * l.size();
                        long[] newShape = l.get(0).shape().clone();
                        if(!step.config().includeMinibatchDim()){
                            newShape = new long[]{0, newShape[0], newShape[1], newShape[2]};
                        }

                        newShape[0] = l.size();
                        boolean direct = !Loader.getPlatform().startsWith("android");
                        ByteBuffer outBuff = direct ? ByteBuffer.allocateDirect(newSize).order(ByteOrder.LITTLE_ENDIAN) : ByteBuffer.allocate(newSize).order(ByteOrder.LITTLE_ENDIAN);
                        nd.getBuffer().rewind();
                        outBuff.put(nd.getBuffer());
                        for( int i=1; i<l.size(); i++ ){
                            SerializedNDArray ndarr = l.get(i).getAs(SerializedNDArray.class);
                            ndarr.getBuffer().rewind();
                            outBuff.put(ndarr.getBuffer());
                        }
                        SerializedNDArray outArr = new SerializedNDArray(l.get(0).type(), newShape, outBuff);
                        d.put(outNames.get(idx++), NDArray.create(outArr));
                    }
                } else {
                    d.putListNDArray(outNames.get(idx++), l);
                }

            } else {
                //Single image case

                Image i = data.getImage(s);

                if (meta) {
                    Pair<NDArray, BoundingBox> p = ImageToNDArray.convertWithMetadata(i, step.config());
                    cropRegionMeta.add(p.getSecond());
                    origHMeta.add((long) i.height());
                    origWMeta.add((long) i.width());
                } else {
                    NDArray array = ImageToNDArray.convert(i, step.config());
                    d.put(outNames.get(idx++), array);
                }
            }
        }

        if(step.keepOtherValues()) {
            for (String s : data.keys()) {
                if(toConvert.contains(s))
                    continue;
                d.copyFrom(s, data);
            }
        }

        if(meta){
            Data dMeta = meta ? Data.empty() : null;
            if(cropRegionMeta.size() == 1){
                //If only 1 image is converted: store as single values
                dMeta.put(ImageToNDArrayStep.META_INNAME_KEY, toConvert.get(0));
                dMeta.put(ImageToNDArrayStep.META_OUTNAME_KEY, outNames.get(0));
                dMeta.put(ImageToNDArrayStep.META_IMG_H, origHMeta.get(0));
                dMeta.put(ImageToNDArrayStep.META_IMG_W, origWMeta.get(0));
                dMeta.put(ImageToNDArrayStep.META_CROP_REGION, cropRegionMeta.get(0));
            } else {
                //Multiple images converted: store as multiple values
                dMeta.putListString(ImageToNDArrayStep.META_INNAME_KEY, toConvert);
                dMeta.putListString(ImageToNDArrayStep.META_OUTNAME_KEY, outNames);
                dMeta.putListInt64(ImageToNDArrayStep.META_IMG_H, origHMeta);
                dMeta.putListInt64(ImageToNDArrayStep.META_IMG_W, origWMeta);
                dMeta.putListBoundingBox(ImageToNDArrayStep.META_CROP_REGION, cropRegionMeta);
            }
            String key = step.metadataKey();
            if(key == null)
                key = ImageToNDArrayStep.DEFAULT_METADATA_KEY;

            Data m = Data.singleton(key, dMeta);       //Note we embed it in a Data instance, to not conflict with other metadata keys
            d.setMetaData(m);
        }

        return d;
    }

    private void empty(Data d, String outName){
        long[] shape = ImageToNDArray.getOutputShape(step.config());
        if(shape.length == 3){
            shape = new long[]{0, shape[0], shape[1], shape[2]};
        } else {
            shape[0] = 0;
        }
        SerializedNDArray arr = new SerializedNDArray(step.config().dataType(), shape, ByteBuffer.allocate(0));
        d.put(outName, NDArray.create(arr));
    }
}
