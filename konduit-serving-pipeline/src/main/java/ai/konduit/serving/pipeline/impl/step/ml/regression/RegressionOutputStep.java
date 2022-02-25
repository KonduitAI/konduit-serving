/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.impl.step.ml.regression;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;


/**
 * RegressionOutput adapter - extracts values from an NDArray to double values in the output Data instance,
 * with names as specified.<br>
 * For example input=Data{'myArray"=<ndarray>}, output=Data{"x"=ndarray[0], "y"=ndarray[7]}
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("REGRESSION_OUTPUT")
@Schema(description = "RegressionOutput adapter - extracts values from an NDArray to double values in the output Data instance," +
        " with names as specified.<br>For example input=Data{\"myArray\"=<ndarray>}, output=Data{\"x\"=ndarray[0], \"y\"=ndarray[7]}" )
public class RegressionOutputStep implements PipelineStep {

    @Schema(description = "inputName - optional. If set: this represents the NDArray. If not set: use DataUtils.inferField to find an NDArray field")
    private String inputName;

    @Schema(description = "Map<String,Integer> where the key is the output name, and the value is the index in the array.", defaultValue = "null")
    private Map<String,Integer> names;

}
