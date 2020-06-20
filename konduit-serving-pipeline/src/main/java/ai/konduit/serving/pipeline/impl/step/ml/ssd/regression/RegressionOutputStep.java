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

package ai.konduit.serving.pipeline.impl.step.ml.ssd.regression;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;


@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName("REGRESSION_OUTPUT")
@Schema(description = "RegressionOutput handler")
public class RegressionOutputStep implements PipelineStep {

    @Schema(description = "inputName - optional. If set: this represents the NDArray. If not set: use DataUtils.inferField to find an NDArray field")
    String inputName;

    @Schema(description = "Map<String,Integer> where the key is the output name, and the value is the index in the array.")
    Map<String,Integer> outputNames;


    public RegressionOutputStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null

    }

}
