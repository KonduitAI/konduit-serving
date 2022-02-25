/* ******************************************************************************
 * Copyright (c) 2022 Konduit K.K.
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
package ai.konduit.serving.python;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Data
@Accessors(fluent = true)
@JsonName("PYTHON")
@NoArgsConstructor
@Schema(description = "A pipeline step that configures a python script that is to be executed.")
public class PythonStep implements PipelineStep {
    @JsonProperty("pythonConfig")
    @Schema(description = "The python configuration associated with this python step. This controls how the python step will be executed. When describing a python configuration from the command line" +
            "Each field is separated by a comma. When describing inputs and outputs (ioInput,ioOutput) ensure that values are surrounded in quotes as string literals." +
            "You can escape a \" with a \\ character. Each input/output is then space separated within the quotes. The format is:" +
            "name, python type, konduit serving value type.")
    private PythonConfig pythonConfig;
}
