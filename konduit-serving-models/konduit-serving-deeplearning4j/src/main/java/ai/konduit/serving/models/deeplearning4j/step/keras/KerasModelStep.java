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

package ai.konduit.serving.models.deeplearning4j.step.keras;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

//TODO move this to somewhere it can be used for non-DL4J execution
@Data
@Accessors(fluent = true)
@Builder
public class KerasModelStep implements PipelineStep {

    private String modelUri;
    private List<String> inputNames;        //Mainly for ComputationGraph with multiple inputs - map Data keys to ComputationGraph outputs
    private List<String> outputNames;       //Mainly for ComputationGraph with multiple outputs - map INDArray[] to Data keys

    public KerasModelStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("inputNames") List<String> inputNames,
                                 @JsonProperty("outputNames") List<String> outputNames){
        this.modelUri = modelUri;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }

}
