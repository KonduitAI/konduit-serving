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
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

/**
 * The SwitchStep forwards the input Data instance to only one of N data instances, using a provided {@link SwitchFn}
 * This can be used to implement conditional operations.
 * Usually this is used in conjunction with an AnyStep: i.e., input -> Switch -> (left branch, right branch) -> Any<br>
 *
 * @author Alex Black
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
@JsonName(GraphConstants.GRAPH_SWITCH_JSON_KEY)
public class SwitchStep extends BaseGraphStep {

    protected String inStep;
    protected SwitchFn switchFn;

    /**
     * @param b        GraphBuilder
     * @param name     Name of this node
     * @param inStep   Name of the input node
     * @param switchFn Switch function to use to decide which output to forward the input to
     */
    public SwitchStep(GraphBuilder b, String name, String inStep, SwitchFn switchFn) {
        super(b, name);
        this.inStep = inStep;
        this.switchFn = switchFn;
    }

    @Override
    public int numInputs() {
        return 1;
    }

    @Override
    public String input() {
        return inStep;
    }

    @Override
    public List<String> inputs() {
        return Collections.singletonList(inStep);
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
    public String toString() {
        return "Switch(fn=" + switchFn + ",inputs=" + inputs() + ")";
    }
}
