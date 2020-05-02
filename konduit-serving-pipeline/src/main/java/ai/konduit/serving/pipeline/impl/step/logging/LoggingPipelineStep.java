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

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.slf4j.event.Level;

@Data
@Builder
public class LoggingPipelineStep implements PipelineStep {

    public enum Log { KEYS, KEYS_AND_VALUES }

    @Builder.Default
    private Level logLevel = Level.INFO;

    @Builder.Default
    private Log log = Log.KEYS;

    @Builder.Default
    public String keyFilterRegex = null;

    public LoggingPipelineStep(@JsonProperty("logLevel") Level logLevel, @JsonProperty("log") Log log, @JsonProperty("keyfilterRegex") String keyFilterRegex) {
        this.logLevel = logLevel;
        this.log = log;
        this.keyFilterRegex = keyFilterRegex;
    }

}
