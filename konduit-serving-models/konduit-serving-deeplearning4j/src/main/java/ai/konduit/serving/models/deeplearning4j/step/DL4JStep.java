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

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@JsonName("DEEPLEARNING4J")
@Schema(description = "A pipeline step that configures a DL4J model that is to be executed.")
public class DL4JStep implements PipelineStep {

    @Schema(description = "Specifies the location of a saved model file.")
    private String modelUri;

    @Schema(description = "A list of names of the input placeholders (mainly for DL4J - computation graph, with multiple inputs. Where values from the input data keys are mapped to " +
            "the computation graph inputs).")
    private List<String> inputNames;

    @Schema(description = "A list of names of the output placeholders (mainly for DL4J - computation graph, with multiple outputs. Where the values of these output keys are mapped " +
            "from the computation graph output - INDArray[] to data keys).")
    private List<String> outputNames;

    @Schema(description = "Optional, usually unnecessary. Specifies a class used to load the model if customization in how " +
            "model loading is performed, instead of the usual MultiLayerNetwork.load or ComputationGraph.load methods. " +
            "Must be a java.util.Function<String,MultiLayerNetwork> or java.util.Function<String,ComputationGraph>")
    private String loaderClass;

    public DL4JStep(@JsonProperty("modelUri") String modelUri, @JsonProperty("inputNames") List<String> inputNames,
                    @JsonProperty("outputNames") List<String> outputNames){
        this.modelUri = modelUri;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }

    @Tolerate
    public DL4JStep inputNames(String... inputNames) {
        return this.inputNames(Arrays.asList(inputNames));
    }

    @Tolerate
    public DL4JStep outputNames(String... outputNames) {
        return this.outputNames(Arrays.asList(outputNames));
    }

}
