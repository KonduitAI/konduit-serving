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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.parallelism.ParallelInference;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * An {@link InferenceExecutioner}
 * for use with the {@link ParallelInference}
 *
 * @author Adam Gibson
 */
@Slf4j
public class MultiLayerNetworkInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<MultiLayerNetwork>, INDArray[], INDArray[], ParallelInferenceConfig, MultiLayerNetwork> {
    @Getter
    private MultiLayerNetwork multiLayerNetwork;
    @Getter
    private ModelLoader<MultiLayerNetwork> modelLoader;

    @Override
    public ModelLoader<MultiLayerNetwork> modelLoader() {
        return modelLoader;
    }

    @Override
    public MultiLayerNetwork model() {
        return multiLayerNetwork;
    }


    @Override
    public void initialize(ModelLoader<MultiLayerNetwork> model, ParallelInferenceConfig parallelInferenceConfig) throws Exception {
        this.multiLayerNetwork = model.loadModel();
        this.modelLoader = model;
    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        synchronized (multiLayerNetwork) {
            return new INDArray[] { multiLayerNetwork.output(input[0]) };
        }
    }

    @Override
    public void stop() {
    }
}
