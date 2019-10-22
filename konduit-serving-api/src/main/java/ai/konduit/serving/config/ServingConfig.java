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

import ai.konduit.serving.metrics.MetricType;
import static ai.konduit.serving.metrics.MetricType.*;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServingConfig {

    @Builder.Default
    private int httpPort = 9008;
    @Builder.Default
    private String listenHost = "localhost";

    @Builder.Default
    private Input.DataType inputDataType = Input.DataType.JSON;
    @Builder.Default
    private Output.DataType outputDataType = Output.DataType.JSON;
    @Builder.Default
    private Output.PredictionType predictionType = Output.PredictionType.CLASSIFICATION;

    @Builder.Default
    private String uploadsDirectory = "file-uploads/";
    @Builder.Default
    private boolean logTimings = false;
    @Builder.Default
    private ParallelInferenceConfig parallelInferenceConfig = ParallelInferenceConfig.defaultConfig();

    private PubsubConfig pubSubConfig;

    @Builder.Default
    private List<MetricType> metricTypes = Arrays.asList(
            CLASS_LOADER,
            JVM_MEMORY,
            JVM_GC,
            PROCESSOR,
            JVM_THREAD,
            LOGGING_METRICS,
            NATIVE
    );
}
