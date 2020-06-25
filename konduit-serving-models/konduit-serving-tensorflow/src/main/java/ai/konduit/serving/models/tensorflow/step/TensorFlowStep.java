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

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.models.tensorflow.TensorFlowConfiguration;
import ai.konduit.serving.pipeline.api.BaseModelPipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@SuperBuilder
@JsonName("TENSORFLOW")
@Schema(description = "A pipeline step that configures a TensorFlow model that is to be executed.")
public class TensorFlowStep extends BaseModelPipelineStep<TensorFlowConfiguration> {

    @Schema(description = "A list of names of the input placeholders.")
    private List<String> inputNames;

    @Schema(description = "A list of names of the output arrays - i.e., what should be predicted.")
    private List<String> outputNames;

    public TensorFlowStep(String modelUri, TensorFlowConfiguration config) {
        super(modelUri, config);
    }

    public TensorFlowStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("config") TensorFlowConfiguration config,
                          @JsonProperty("inputNames") List<String> inputNames, @JsonProperty("outputNames") List<String> outputNames){
        super(modelUri, config);
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }
}
