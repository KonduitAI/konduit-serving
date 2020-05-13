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

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.data.ValueType;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.nd4j.common.base.Preconditions;

import java.util.List;

public class ShowImageStepRunner implements PipelineStepRunner {

    private static final int MIN_HEIGHT = 64;
    private static final int MIN_WIDTH = 64;

    private ShowImagePipelineStep step;
    private boolean initialized;
    private CanvasFrame canvas;

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
        String name = step.getImageName();
        if(name == null)
            name = tryInferName(data);

        Preconditions.checkState(data.has(name) && data.type(name) == ValueType.IMAGE, "Data does not have image value for name \"%s\" - data keys = %s",
                name, data.keys());

        Image i = data.getImage(name);
        Frame f = i.getAs(Frame.class);

        if(!initialized)
            init();

        canvas.showImage(f);
        if(step.getWidth() == 0 || step.getHeight() == 0){
            canvas.setCanvasSize(Math.max(MIN_WIDTH, i.width()), Math.max(MIN_HEIGHT, i.height()));
        }

        return data;
    }

    protected synchronized void init(){
        canvas = new CanvasFrame(step.getDisplayName());
        canvas.setCanvasSize(step.getWidth(), step.getHeight());
        initialized = true;
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
