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

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An {@link InferenceExecutioner}
 * for use with the {@link ParallelInference}
 *
 * @author Adam Gibson
 */
@Slf4j
public class MultiLayerNetworkInferenceExecutioner implements
        InferenceExecutioner<ModelLoader<MultiLayerNetwork>,INDArray[],INDArray[], ParallelInferenceConfig,MultiLayerNetwork> {

    private ParallelInference parallelInference;
    @Getter
    private MultiLayerNetwork multiLayerNetwork;
    private ReentrantReadWriteLock modelReadWriteLock;
    private static Field zooField,protoModelField,replicateModelField;
    @Getter
    private ModelLoader<MultiLayerNetwork> modelLoader;

    static {
        try {
            zooField =  ParallelInference.class.getDeclaredField("zoo");
            zooField.setAccessible(true);
        } catch (Exception e) {
            log.error("Unable to access zoo field.");
        }
    }


    @Override
    public ModelLoader<MultiLayerNetwork> modelLoader() {
        return modelLoader;
    }

    @Override
    public MultiLayerNetwork model() {
        try {
            modelReadWriteLock.readLock().lock();
            return multiLayerNetwork;
        } finally {
            modelReadWriteLock.readLock().unlock();
        }
    }


    @Override
    public void initialize(ModelLoader<MultiLayerNetwork> model, ParallelInferenceConfig parallelInferenceConfig) throws Exception {
        MultiLayerNetwork multiLayerNetwork = model.loadModel();
        this.multiLayerNetwork = multiLayerNetwork;
        this.modelLoader = model;
        ParallelInference inference = new ParallelInference.Builder(multiLayerNetwork)
                .batchLimit(parallelInferenceConfig.getBatchLimit())
                .queueLimit(parallelInferenceConfig.getQueueLimit())
                .inferenceMode(parallelInferenceConfig.getInferenceMode())
                .workers(parallelInferenceConfig.getWorkers())
                .build();

        this.parallelInference = inference;

        Object[] zoo = (Object[]) zooField.get(parallelInference);
        if(protoModelField == null) {
            protoModelField = zoo[0].getClass().getDeclaredField("protoModel");
            protoModelField.setAccessible(true);
        }

        if(replicateModelField == null) {
            replicateModelField = zoo[0].getClass().getDeclaredField("replicatedModel");
            replicateModelField.setAccessible(true);
        }

        modelReadWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public INDArray[] execute(INDArray[] input) {
        if(parallelInference == null) {
            throw new IllegalStateException("Initialize not called. No ParallelInference found. Please call inferenceExecutioner.initialize(..)");
        }

        try {
            modelReadWriteLock.readLock().lock();
            INDArray[] output = parallelInference.output(input);
            return output;

        }finally {
            modelReadWriteLock.readLock().unlock();
        }

    }

    @Override
    public void stop() {
        if(parallelInference != null) {
            parallelInference.shutdown();
        }
    }
}
