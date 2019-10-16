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

import ai.konduit.serving.model.loader.ModelLoader;
import ai.konduit.serving.config.ParallelInferenceConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.parallelism.ParallelInference;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An {@link InferenceExecutioner}
 * using {@link org.deeplearning4j.nn.graph.ComputationGraph}
 * and {@link ParallelInference} for multi threaded inference.
 *
 * @author Adam Gibson
 */
@Slf4j
public class ComputationGraphInferenceExecutioner implements InferenceExecutioner<ModelLoader<ComputationGraph>,INDArray,INDArray, ParallelInferenceConfig,ComputationGraph> {

    @Getter
    private ParallelInference parallelInference;
    @Getter
    private ComputationGraph computationGraph;
    private static Field zooField,protoModelField,replicateModelField;
    private ReentrantReadWriteLock modelReadWriteLock;
    @Getter
    private ModelLoader<ComputationGraph> computationGraphModelLoader;

    static {
        try {
            zooField =  ParallelInference.class.getDeclaredField("zoo");
            zooField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    @Override
    public ModelLoader<ComputationGraph> modelLoader() {
        return computationGraphModelLoader;
    }

    @Override
    public ComputationGraph model() {
        try {
            modelReadWriteLock.readLock().lock();
            return computationGraph;
        } finally {
            modelReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public void initialize(ModelLoader<ComputationGraph> model, ParallelInferenceConfig parallelInferenceConfig) throws Exception {
        ComputationGraph computationGraph = model.loadModel();
        this.computationGraph = computationGraph;
        this.computationGraphModelLoader = model;

        ParallelInference inference = new ParallelInference.Builder(computationGraph)
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
    public INDArray execute(INDArray input) {
        if(parallelInference == null) {
            throw new IllegalStateException("Initialize not called. No ParallelInference found. Please call inferenceExecutioner.initialize(..)");
        }

        try {
            modelReadWriteLock.readLock().lock();
            INDArray output =  parallelInference.output(new INDArray[]{input})[0];
            return output;

        }
        finally {
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
