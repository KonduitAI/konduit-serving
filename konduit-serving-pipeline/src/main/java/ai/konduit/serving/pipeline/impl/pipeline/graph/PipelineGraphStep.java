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

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.*;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;
import org.nd4j.shade.jackson.annotation.JsonUnwrapped;

import java.util.Collections;
import java.util.List;

/**
 * A {@link GraphStep} that contains a {@link PipelineStep}
 *
 * @author Alex Black
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties("builder")
public class PipelineGraphStep extends BaseGraphStep {
    private GraphBuilder builder;
    @JsonUnwrapped
    private PipelineStep step;
    private String name;
    private String input;

    public PipelineGraphStep(GraphBuilder builder, PipelineStep step, String name, String input) {
        super(builder, name);
        this.step = step;
        this.input = input;
    }

    @Override
    public int numInputs() {
        return 1;
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public List<String> inputs() {
        return Collections.singletonList(input);
    }

    @Override
    public boolean hasStep() {
        return true;
    }

    @Override
    public String toString() {
        return step.toString();
    }
}
