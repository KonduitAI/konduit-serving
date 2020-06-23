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

package ai.konduit.serving.config;


import ai.konduit.serving.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.deeplearning4j.parallelism.ParallelInference;
import org.deeplearning4j.parallelism.inference.InferenceMode;

import java.io.Serializable;

/**
 * Parallel inference configuration configuration
 *
 * @author Adam Gibson
 */
@Data
@Accessors(fluent=true)
@AllArgsConstructor
@NoArgsConstructor
public class ParallelInferenceConfig implements Serializable, TextConfig {

    private static final int NUM_WORKERS = 1;

    private int queueLimit = ParallelInference.DEFAULT_QUEUE_LIMIT;
    private int batchLimit = ParallelInference.DEFAULT_BATCH_LIMIT;
    private int workers = NUM_WORKERS;
    private int maxTrainEpochs = 1;
    private InferenceMode inferenceMode = ParallelInference.DEFAULT_INFERENCE_MODE;

    //config json for vertx: used for configuring
    //the retrainer and revision manager
    private String vertxConfigJson;

    /**
     * Default parallel inference configuration
     *
     * @return default configuration
     */
    public static ParallelInferenceConfig defaultConfig() {
        return new ParallelInferenceConfig()
                .inferenceMode(ParallelInference.DEFAULT_INFERENCE_MODE)
                .queueLimit(ParallelInference.DEFAULT_QUEUE_LIMIT)
                .batchLimit(ParallelInference.DEFAULT_BATCH_LIMIT)
                .workers(NUM_WORKERS);

    }

    public static ParallelInferenceConfig fromJson(String json){
        return ObjectMappers.fromJson(json, ParallelInferenceConfig.class);
    }

    public static ParallelInferenceConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, ParallelInferenceConfig.class);
    }
}
