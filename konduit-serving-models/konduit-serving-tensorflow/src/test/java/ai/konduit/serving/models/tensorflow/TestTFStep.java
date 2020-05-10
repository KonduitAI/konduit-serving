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

package ai.konduit.serving.models.tensorflow;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.convert.config.NDChannelLayout;
import ai.konduit.serving.data.image.convert.config.NDFormat;
import ai.konduit.serving.data.image.step.ndarray.ImageToNDArrayStep;
import ai.konduit.serving.models.tensorflow.step.TensorFlowPipelineStep;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import org.junit.Test;

import java.io.File;

public class TestTFStep {

    @Test
    public void testFrozenModel(){

        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        File f = new File("C:/Users/Alex/Downloads/frozen_inference_graph_face.pb");

        Pipeline p = SequencePipeline.builder()
                .add(ImageToNDArrayStep.builder()
                        .config(c)
                    .build())
                .add(TensorFlowPipelineStep.builder()
                        .modelUri(f.toURI().toString())      //Face detection model
                .build())
                .build();

        Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

        PipelineExecutor exec = p.executor();

        Data in = Data.singleton("image", img);

        Data out = exec.exec(null, in);
    }

    @Test
    public void testSavedModel(){
        ImageToNDArrayConfig c = ImageToNDArrayConfig.builder()
                .height(256)
                .width(256)
                .channelLayout(NDChannelLayout.RGB)
                .includeMinibatchDim(true)
                .format(NDFormat.CHANNELS_LAST)
                .dataType(NDArrayType.UINT8)
                .normalization(null)
                .build();

        File f = new File("C:\\Temp\\tf_test\\mobilenetv1_saved_model.pb");

        Pipeline p = SequencePipeline.builder()
                .add(ImageToNDArrayStep.builder()
                        .config(c)
                        .build())
                .add(TensorFlowPipelineStep.builder()
                        .modelUri(f.toURI().toString())      //Face detection model
                        .build())
                .build();

        Image img = Image.create(new File("C:/Users/Alex/Downloads/testimage.png"));

        PipelineExecutor exec = p.executor();

        Data in = Data.singleton("image", img);

        Data out = exec.exec(null, in);

    }

}
