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

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.convert.ImageToNDArray;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.NonNull;
import org.nd4j.common.base.Preconditions;
import org.nd4j.common.primitives.Pair;

import java.util.ArrayList;
import java.util.List;

@CanRun(value = ImageToNDArrayStep.class, moduleName = "konduit-serving-image")
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

        boolean meta = step.isMetadata();
        List<BoundingBox> cropRegionMeta = meta ? new ArrayList<>(toConvert.size()) : null;
        List<Long> origHMeta = meta ? new ArrayList<>(toConvert.size()) : null;
        List<Long> origWMeta = meta ? new ArrayList<>(toConvert.size()) : null;

        Data d = Data.empty();
        int idx = 0;
        for(String s : toConvert){
            Image i = data.getImage(s);

            if(meta){
                Pair<NDArray,BoundingBox> p = ImageToNDArray.convertWithMetadata(i, step.getConfig());
                cropRegionMeta.add(p.getSecond());
                origHMeta.add((long)i.height());
                origWMeta.add((long)i.width());
            } else {
                NDArray array = ImageToNDArray.convert(i, step.getConfig());
                d.put(outNames.get(idx++), array);
            }
        }

        if(step.isKeepOtherValues()) {
            for (String s : data.keys()){
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
            String key = step.getMetadataKey();
            if(key == null)
                key = ImageToNDArrayStep.DEFAULT_METADATA_KEY;

            Data m = Data.singleton(key, dMeta);       //Note we embed it in a Data instance, to not conflict with other metadata keys
            d.setMetaData(m);
        }

        return d;
    }
}
