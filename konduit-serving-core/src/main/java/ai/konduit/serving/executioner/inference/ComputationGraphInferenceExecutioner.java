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
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.parallelism.ParallelInference;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * An {@link InferenceExecutioner}
 * using {@link org.deeplearning4j.nn.graph.ComputationGraph}
 * and {@link ParallelInference} for multi threaded inference.
 *
 * @author Adam Gibson
 */
@Slf4j
public class ComputationGraphInferenceExecutioner implements InferenceExecutioner<ModelLoader<ComputationGraph>, INDArray, INDArray, ParallelInferenceConfig, ComputationGraph> {
    @Getter
    private ComputationGraph computationGraph;

    @Getter
    private ModelLoader<ComputationGraph> computationGraphModelLoader;

    @Override
    public ModelLoader<ComputationGraph> modelLoader() {
        return computationGraphModelLoader;
    }

    @Override
    public ComputationGraph model() {
        return computationGraph;
    }

    @Override
    public void initialize(ModelLoader<ComputationGraph> model, ParallelInferenceConfig parallelInferenceConfig) throws Exception {
        this.computationGraph = model.loadModel();
        this.computationGraphModelLoader = model;
    }

    @Override
    public INDArray execute(INDArray input) {
        synchronized (computationGraph) {
            return computationGraph.output(input)[0];
        }

    }

    @Override
    public void stop() {
    }
}
