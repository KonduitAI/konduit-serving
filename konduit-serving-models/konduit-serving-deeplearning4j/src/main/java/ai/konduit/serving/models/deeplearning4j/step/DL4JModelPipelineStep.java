/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.models.deeplearning4j.step;

import ai.konduit.serving.models.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@Accessors(fluent = true)
@Builder
public class DL4JModelPipelineStep implements PipelineStep {

    private String modelUri;
    private List<String> inputNames;        //Mainly for ComputationGraph with multiple inputs - map Data keys to ComputationGraph outputs
    private List<String> outputNames;       //Mainly for ComputationGraph with multiple outputs - map INDArray[] to Data keys

    public DL4JModelPipelineStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("inputNames") List<String> inputNames,
                                 @JsonProperty("outputNames") List<String> outputNames){
        this.modelUri = modelUri;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }
}
