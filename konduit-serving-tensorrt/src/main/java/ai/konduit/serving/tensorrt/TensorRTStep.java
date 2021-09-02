/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.tensorrt;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@SuperBuilder
@Data
@Accessors(fluent = true)
@JsonName("TENSORRT")
@NoArgsConstructor
@Schema(description = "A pipeline step that configures a python script that is to be executed.")
public class TensorRTStep implements PipelineStep {

    @Schema(description = "Specifies the location of a saved model file.")
    private String modelUri;

    @Schema(description = "A list of names of the input placeholders ( computation graph, with multiple inputs. Where values from the input data keys are mapped to " +
            "the computation graph inputs).")
    private List<String> inputNames;

    @Schema(description = "A list of names of the output placeholders (computation graph, with multiple outputs. Where the values of these output keys are mapped " +
            "from the computation graph output - INDArray[] to data keys).")
    private List<String> outputNames;

    @Schema(description = "The batch size for the runtime")
    private int batchSize;
    @Schema(description = "Whether to use fp16 or not")
    private boolean useFp16;

    @Schema(description = "The max workspace size to use")
    private long maxWorkspaceSize;

    @Schema(description = "The min expected dimensions to optimize for")
    private Map<String,long[]> minDimensions;
    @Schema(description = "The max expected dimensions to optimize for")
    private Map<String,long[]> maxDimensions;
    @Schema(description = "The optimal expected dimensions to optimize for")
    private Map<String,long[]> optimalDimensions;
    @Schema(description = "The output dimensions for each output, minus the batch size, eg: if an image is NCHW only include CHW")
    private Map<String,long[]> outputDimensions;


}
