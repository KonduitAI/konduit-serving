/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.models.tensorflow.step;

import ai.konduit.serving.annotation.JsonName;
import ai.konduit.serving.models.tensorflow.TensorFlowConfiguration;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@SuperBuilder
@JsonName(jsonName = "TENSORFLOW", subclassOf = PipelineStep.class)
public class TensorFlowPipelineStep extends BaseModelPipelineStep<TensorFlowConfiguration> {

    private List<String> inputNames;
    private List<String> outputNames;

    public TensorFlowPipelineStep(String modelUri, TensorFlowConfiguration config) {
        super(modelUri, config);
    }

    public TensorFlowPipelineStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("config") TensorFlowConfiguration config,
                                 @JsonProperty("inputNames") List<String> inputNames, @JsonProperty("outputNames") List<String> outputNames){
        super(modelUri, config);
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }
}
