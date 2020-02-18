/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019-2020 Konduit AI.
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

import org.nd4j.linalg.api.ndarray.INDArray;

import org.bytedeco.onnxruntime.Session;

import java.util.List;
import java.util.Map;

/**
 * An {@link InferenceExecutioner}
 * for use with the {@link ONNXThreadPool}
 *
 * @author Adam Gibson, Alex Merritt
 */
@Slf4j
public class OnnxInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<Session>, List<Map<String, INDArray>>, List<Map<String, INDArray>>,
                ParallelInferenceConfig, Session> {

    @Getter
    private ONNXThreadPool inference;
    @Getter
    private ModelLoader<Session> modelLoader;


    @Override
    public ModelLoader<Session> modelLoader() {
        return modelLoader;
    }

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
        this.inference = new ONNXThreadPool.Builder(model)
                .batchLimit(config.getBatchLimit())
                .queueLimit(config.getQueueLimit())
                .inferenceMode(config.getInferenceMode())
                .workers(config.getWorkers())
                .build();
        this.modelLoader = model;


    }

    @Override
    public List<Map<String, INDArray>> execute(List<Map<String, INDArray>> input) {
        if (inference == null) {
            throw new IllegalStateException("Initialize not called. No ParallelInference found. Please call " +
                    "inferenceExecutioner.initialize(..)");
        }
        return inference.output(input);
    }

    @Override
    public void stop() {
        if (inference != null) {
            inference.shutdown();
        }
    }
}
