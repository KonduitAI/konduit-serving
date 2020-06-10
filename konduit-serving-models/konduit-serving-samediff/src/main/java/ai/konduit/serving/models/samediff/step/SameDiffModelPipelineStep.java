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
package ai.konduit.serving.models.samediff.step;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.models.samediff.SameDiffConfig;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@SuperBuilder
@Data
@Accessors(fluent = true)
@JsonName("SAMEDIFF")
public class SameDiffModelPipelineStep implements PipelineStep {

    private String modelUri;
    private List<String> outputNames;       //Mainly for ComputationGraph with multiple outputs - map INDArray[] to Data keys

    public SameDiffModelPipelineStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("outputNames") List<String> outputNames){
        this.modelUri = modelUri;
        this.outputNames = outputNames;
    }
}
