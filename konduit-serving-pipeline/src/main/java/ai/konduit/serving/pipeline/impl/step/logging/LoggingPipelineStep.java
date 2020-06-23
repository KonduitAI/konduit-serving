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
package ai.konduit.serving.pipeline.impl.step.logging;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.slf4j.event.Level;

/**
 * LoggingPipelineStep simply logs the input Data keys (and optionally values) and returns the input data unchanged
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent=true)
@NoArgsConstructor
@JsonName("LOGGING")
@Schema(description = "A pipeline step that simply logs the input Data keys (and optionally values) and returns " +
        "the input data unchanged.")
public class LoggingPipelineStep implements PipelineStep {

    @Schema(description = "An enum specifying what part of a data instance should be logged. <br><br>" +
            "KEYS -> only output data keys, <br>" +
            "KEYS_AND_VALUES -> output both data keys and values.")
    public enum Log { KEYS, KEYS_AND_VALUES }

    @Schema(description = "Log level. This is similar to how standard logging frameworks define logging categories.",
            defaultValue = "INFO")
    private Level logLevel = Level.INFO;

    @Schema(description = "An enum specifying what part of a data instance should be logged.", defaultValue = "KEYS")
    private Log log = Log.KEYS;

    @Schema(description = "A regular expression that allows filtering of keys - i.e., only those that match the regex will be logged.")
    public String keyFilterRegex = null;

    public LoggingPipelineStep(@JsonProperty("logLevel") Level logLevel, @JsonProperty("log") Log log, @JsonProperty("keyfilterRegex") String keyFilterRegex) {
        this.logLevel = logLevel;
        this.log = log;
        this.keyFilterRegex = keyFilterRegex;
    }

}
