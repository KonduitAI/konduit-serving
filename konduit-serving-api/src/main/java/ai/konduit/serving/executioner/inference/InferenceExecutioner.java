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

/**
 * An {@link InferenceExecutioner}
 * handles wrapping a model thread pool
 * such as {@link org.deeplearning4j.parallelism.ParallelInference}
 * for thread safe inference allowing different modes of inference
 * including batch accumulation or 1 at a time processing of data
 *
 * This includes handling a specific model type,
 * a specified input type such as {@link org.nd4j.linalg.api.ndarray.INDArray}
 * and a configuration such as the {@link ParallelInferenceConfig}
 *
 * @param <MODEL_LOADER_TYPE>  the model loader type to use for loading models of MODEL_TYPE
 * @param <INPUT_TYPE> the input data to the threadpool
 * @param <OUTPUT_TYPE> the output type of the threadpool
 * @param <CONFIG_TYPE> the configuration for the thread pool
 * @param <MODEL_TYPE> the model type to use for inference
 *
 * @author Adam Gibson
 */
public interface InferenceExecutioner<MODEL_LOADER_TYPE extends ModelLoader<MODEL_TYPE>,INPUT_TYPE,OUTPUT_TYPE,CONFIG_TYPE,MODEL_TYPE> {


    /**
     * Returns the {@link ModelLoader}
     * for this inference executioner
     * @return a model loader relative to the type
     * used for inference
     */
    MODEL_LOADER_TYPE modelLoader();

    /**
     * Returns the underlying model used for execution.
     * @return the underlying model for execution
     */
    MODEL_TYPE model();


    /**
     * Initialize a thread pool based on the given configuration
     * and the passed in model
     * @param model the model to initialize with
     * @param config the configuration for the thread pool
     * @throws Exception exception
     */
    void initialize(MODEL_LOADER_TYPE model, CONFIG_TYPE config) throws Exception;


    /**
     *  Executes inference on the configuration
     *
     * @param input the input to score
     * @return the output of the model
     */
    OUTPUT_TYPE execute(INPUT_TYPE input);

    /**
     * Stop the inference executioner.
     */
    void stop();
}
