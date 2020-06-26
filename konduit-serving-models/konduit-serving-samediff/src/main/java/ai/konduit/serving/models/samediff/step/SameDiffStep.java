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
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@SuperBuilder
@Data
@Accessors(fluent = true)
@JsonName("SAMEDIFF")
@Schema(description = "A pipeline step that configures a SameDiff model that is to be executed.")
public class SameDiffStep implements PipelineStep {

    @Schema(description = "Specifies the location of a saved model file.")
    private String modelUri;

    @Schema(description = "A list of names of the output arrays - i.e., the names of the arrays to predict/return.")
    private List<String> outputNames;

    public SameDiffStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("outputNames") List<String> outputNames){
        this.modelUri = modelUri;
        this.outputNames = outputNames;
    }
}
