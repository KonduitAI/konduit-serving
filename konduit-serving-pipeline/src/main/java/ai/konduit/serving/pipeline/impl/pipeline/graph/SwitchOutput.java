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

package ai.konduit.serving.pipeline.impl.pipeline.graph;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * SwitchOutput simply represents one output branch of a {@link SwitchStep}
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@JsonName(GraphConstants.GRAPH_SWITCH_OUTPUT_JSON_KEY)
@Schema(description = "A graph pipeline step that is a single item in a list of multiple switch step outputs.")
public class SwitchOutput extends BaseGraphStep implements GraphStep {

    @Schema(description = "Number of outputs from the switch step.")
    private final int outputNum;

    @Schema(description = "Name of the switch output step.")
    private final String switchName;

    /**
     * @param b          GraphBuilder
     * @param name       Name of the output node
     * @param switchName Name of the Switch node that this SwitchOutput represents the output for
     * @param outputNum  Output number of the SwitchStep that this is for
     */
    public SwitchOutput(@JsonProperty("GraphBuilder") GraphBuilder b, @JsonProperty("name") String name, @JsonProperty("switchName") String switchName, @JsonProperty("outputNum") int outputNum){
        super(b, name);
        this.switchName = switchName;
        this.outputNum = outputNum;
    }


    @Override
    public int numInputs() {
        return 1;
    }

    @Override
    public String input() {
        return switchName;
    }

    @Override
    public List<String> inputs() {
        return Collections.singletonList(input());
    }

    @Override
    public boolean hasStep() {
        return false;
    }

    @Override
    public PipelineStep getStep() {
        return null;
    }

    @Override
    public String toString(){
        return "SwitchOutput(\"" + input() + "\"," + outputNum + ")";
    }
}
