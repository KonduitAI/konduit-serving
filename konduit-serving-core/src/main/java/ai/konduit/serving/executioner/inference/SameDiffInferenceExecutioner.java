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
import ai.konduit.serving.model.loader.samediff.SameDiffModelLoader;
import ai.konduit.serving.threadpool.samediff.SameDiffThreadPool;
import ai.konduit.serving.threadpool.tensorflow.TensorFlowThreadPool;
import lombok.Getter;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * An {@link InferenceExecutioner}
 * for use with the {@link TensorFlowThreadPool}
 *
 * @author Adam Gibson
 */
public class SameDiffInferenceExecutioner implements InferenceExecutioner<ModelLoader<SameDiff>, INDArray[], INDArray[],
        ParallelInferenceConfig, SameDiff> {

    @Getter
    private SameDiffThreadPool sameDiffThreadPool;
    @Getter
    private ModelLoader<SameDiff> modelLoader;


    @Override
    public ModelLoader<SameDiff> modelLoader() {
        return modelLoader;
    }

    @Override
    public SameDiff model() {
        try {
            return modelLoader.loadModel();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void initialize(ModelLoader<SameDiff> model, ParallelInferenceConfig config) {
        SameDiffModelLoader sameDiffModelLoader = (SameDiffModelLoader) model;
        sameDiffThreadPool = new SameDiffThreadPool
                .Builder(model).workers(config.getWorkers())
                .inferenceMode(config.getInferenceMode())
                .queueLimit(config.getQueueLimit())
                .batchLimit(config.getBatchLimit())
                .inputNames(sameDiffModelLoader.getInputNames())
                .outputNames(sameDiffModelLoader.getOutputNames())
                .build();
        this.modelLoader = model;

    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        INDArray[] ret = sameDiffThreadPool.output(input);
        for (int i = 0; i < ret.length; i++) {
            if (ret[i].dataType() != Nd4j.dataType()) {
                ret[i] = Nd4j.dataType() == DataType.DOUBLE
                        ? ret[i].castTo(DataType.DOUBLE)
                        : ret[i].castTo(DataType.FLOAT);
            }
        }

        return ret;
    }

    @Override
    public void stop() {
        if (sameDiffThreadPool != null) {
            sameDiffThreadPool.shutdown();
        }
    }
}
