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

package ai.konduit.serving.data.image.step.show;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.data.ValueType;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.nd4j.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CanRun(ShowImagePipelineStep.class)
public class ShowImageStepRunner implements PipelineStepRunner {

    private static final int MIN_HEIGHT = 64;
    private static final int MIN_WIDTH = 64;

    private ShowImagePipelineStep step;
    private boolean initialized;
    private List<CanvasFrame> canvas;

    public ShowImageStepRunner(ShowImagePipelineStep step){
        this.step = step;
    }

    @Override
    public synchronized void close() {
        initialized = false;
        canvas = null;
    }

    @Override
    public PipelineStep getPipelineStep() {
        return step;
    }

    @Override
    public synchronized Data exec(Context ctx, Data data) {
        String name = step.imageName();
        if(name == null)
            name = tryInferName(data);

        boolean allowMultiple = step.allowMultiple();
        boolean isSingle = data.has(name) && data.type(name) == ValueType.IMAGE;
        boolean validList = data.has(name) && data.type(name) == ValueType.LIST && data.listType(name) == ValueType.IMAGE
                && (allowMultiple || data.getListImage(name).size() == 1);


        if(allowMultiple){
            Preconditions.checkState(isSingle || validList,
                    "Data does not have Image value or List<Image> for name \"%s\" - data keys = %s", name, data.keys());
        } else {
            Preconditions.checkState(isSingle || validList,
                    "Data does not have image value (or size 1 List<Image>, given ShowImagePipeline) for name \"%s\" - data keys = %s",
                    name, data.keys());
        }

        List<Image> l;
        if(isSingle)
            l = Collections.singletonList(data.getImage(name));
        else
            l = data.getListImage(name);

        if(!initialized)
            init();

        if(isSingle){
            Image i = l.get(0);
            Frame f = i.getAs(Frame.class);
            canvas.get(0).showImage(f);
            if(step.width() == null || step.height() == null || step.width() == 0 || step.height() == 0){
                canvas.get(0).setCanvasSize(Math.max(MIN_WIDTH, i.width()), Math.max(MIN_HEIGHT, i.height()));
            }
        } else {
            if(!initialized)
                init();

            for( int i=0; i<l.size(); i++ ){
                Image img = l.get(i);
                Frame f = img.getAs(Frame.class);

                if(canvas.size() <= i)
                    canvas.add(newFrame(step.displayName() + "_" + i));


                CanvasFrame cf = canvas.get(i);
                cf.showImage(f);
                if(step.width() == null || step.height() == null || step.width() == 0 || step.height() == 0){
                    cf.setCanvasSize(Math.max(MIN_WIDTH, img.width()), Math.max(MIN_HEIGHT, img.height()));
                }
            }
        }


        return data;
    }

    protected synchronized void init() {
        canvas = new ArrayList<>();
        canvas.add(newFrame(step.displayName()));
        initialized = true;
    }

    protected CanvasFrame newFrame(String name){
        CanvasFrame cf = new CanvasFrame(name);
        int w = (step.width() == null || step.width() == 0) ? MIN_WIDTH : step.width();
        int h = (step.height() == null || step.height() == 0) ? MIN_HEIGHT : step.height();
        cf.setCanvasSize(w, h);
        return cf;
    }

    protected String tryInferName(Data d){
        List<String> l = d.keys();
        String name = null;
        for(String s : l){
            if(d.type(s) == ValueType.IMAGE){
                if(name != null){
                    //Multiple possible images
                    //TODO do we just want to add an option to show multiple images
                    throw new IllegalStateException("No image name/key was provided and multiple images are present in data: \""
                            + name + "\" and \"" + s + "\"" );
                } else {
                    name = s;
                }
            }
        }

        Preconditions.checkState(name != null, "Data does not contain any image values to display. Data keys: %s", d.keys());
        return name;
    }
}
