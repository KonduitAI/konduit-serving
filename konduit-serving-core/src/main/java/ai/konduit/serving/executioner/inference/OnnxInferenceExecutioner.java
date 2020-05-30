/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
import ai.konduit.serving.threadpool.onnx.ONNXThreadPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.onnxruntime.AllocatorWithDefaultOptions;
import org.bytedeco.onnxruntime.Session;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An {@link InferenceExecutioner}
 * for use with the {@link ONNXThreadPool}
 *
 * @author Adam Gibson, Alex Merritt
 */
@Slf4j
public class OnnxInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<Session>, INDArray[], INDArray[], ParallelInferenceConfig, Session> {

    @Getter
    private ONNXThreadPool inference;
    @Getter
    private ModelLoader<Session> modelLoader;

    @Override
    public ModelLoader<Session> modelLoader() {
        return modelLoader;
    }

    private Session model;

    @Override
    public Session model() {
        try {
            return modelLoader.loadModel();
        } catch (Exception e) {
            log.error("Unable to load model in model() call for onnx inference executioner", e);
            return null;
        }
    }

    @Override
    public void initialize(ModelLoader<Session> model, ParallelInferenceConfig config) {
        this.modelLoader = model;
        this.model = model();
        this.inference = new ONNXThreadPool.Builder(model)
                .batchLimit(config.getBatchLimit())
                .queueLimit(config.getQueueLimit())
                .inferenceMode(config.getInferenceMode())
                .workers(config.getWorkers())
                .build();
    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        Preconditions.checkNotNull(input, "Inputs must not be null!");
        Preconditions.checkNotNull(this.model, "Model must not be null!");
        Preconditions.checkState(input.length == this.model.GetInputCount(), "Number of inputs %s did not equal number of model inputs %s!", input.length, model.GetInputCount());
        synchronized (this.model) {
            try (AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions()) {
                Map<String, INDArray> inputs = new LinkedHashMap<>(input.length);

                for (int i = 0; i < this.model.GetInputCount(); i++) {
                    inputs.put(this.model.GetInputName(i, allocator.asOrtAllocator()).getString(), input[i]);
                }

                Map<String, INDArray> ret = inference.output(inputs);
                return ret.values().toArray(new INDArray[0]);
            }
        }
    }

    @Override
    public void stop() {
        if (inference != null) {
            inference.shutdown();
        }
    }
}
