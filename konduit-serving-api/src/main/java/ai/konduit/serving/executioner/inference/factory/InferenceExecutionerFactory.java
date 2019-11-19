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

package ai.konduit.serving.executioner.inference.factory;

import ai.konduit.serving.executioner.inference.InferenceExecutioner;
import ai.konduit.serving.executioner.inference.InitializedInferenceExecutionerConfig;
import ai.konduit.serving.pipeline.step.ModelPipelineStep;

/**
 * Creates {@link InferenceExecutioner}
 * instances for use within pipelines.
 *
 * @author Adam Gibson
 */
public interface InferenceExecutionerFactory {

    /**
     * Create an initialized inference executioner
     * with the proper meta data such as input
     * and output names needed for doing inference
     *
     * @param modelPipelineStepConfig the configuration to initialize with
     * @return the initialized {@link InitializedInferenceExecutionerConfig}
     * @throws Exception in an initialization error happens
     */
    InitializedInferenceExecutionerConfig create(ModelPipelineStep modelPipelineStepConfig) throws Exception;

}
