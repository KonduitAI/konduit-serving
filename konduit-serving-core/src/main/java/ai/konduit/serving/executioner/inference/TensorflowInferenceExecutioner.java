/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.executioner.inference;

import ai.konduit.serving.config.ParallelInferenceConfig;
import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.model.loader.tensorflow.TensorflowGraphHolder;
import ai.konduit.serving.threadpool.tensorflow.TensorFlowThreadPool;
import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;


/**
 * An {@link InferenceExecutioner}
 * for use with the {@link TensorFlowThreadPool}
 *
 * @author Adam Gibson
 */
public class TensorflowInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<TensorflowGraphHolder>,INDArray[],INDArray[],
                ParallelInferenceConfig,TensorflowGraphHolder> {

    @Getter
    private TensorFlowThreadPool tensorflowThreadPool;
    @Getter
    private ModelLoader<TensorflowGraphHolder> modelLoader;


    @Override
    public ModelLoader<TensorflowGraphHolder> modelLoader() {
        return modelLoader;
    }

    @Override
    public TensorflowGraphHolder model() {
        try {
            return modelLoader.loadModel();
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    public void initialize(ModelLoader<TensorflowGraphHolder> model, ParallelInferenceConfig config) {
        tensorflowThreadPool = new TensorFlowThreadPool
                .Builder(model).workers(config.getWorkers())
                .inferenceMode(config.getInferenceMode())
                .queueLimit(config.getQueueLimit())
                .batchLimit(config.getBatchLimit())
                .build();
        this.modelLoader = model;

    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        return tensorflowThreadPool.output(input);
    }

    @Override
    public void stop() {
        if(tensorflowThreadPool != null) {
            tensorflowThreadPool.shutdown();
        }
    }
}
