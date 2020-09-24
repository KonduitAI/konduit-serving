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

package ai.konduit.serving.models.nd4j.tensorflow.step;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@JsonName("ND4JTENSORFLOW")
@Schema(description = "A pipeline step that configures a TensorFlow model that is to be executed based on nd4j graph runner.")
public class Nd4jTensorFlowStep implements PipelineStep {

    @Schema(description = "A list of names of the input placeholders.")
    private List<String> inputNames;


    @Schema(description = "A list of names of the output arrays - i.e., what should be predicted.")
    private List<String> outputNames;

    @Schema(description = "A map of constants")
    private Map<String, INDArray> constants = new HashMap<>();


    @Schema(description = "Uniform Resource Identifier of model")
    private String modelUri;






    @Tolerate
    public Nd4jTensorFlowStep inputNames(String... inputNames) {
        return this.inputNames(Arrays.asList(inputNames));
    }

    @Tolerate
    public Nd4jTensorFlowStep outputNames(String... outputNames) {
        return this.outputNames(Arrays.asList(outputNames));
    }
}

