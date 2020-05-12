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
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MergeStep implements GraphStep {

    private GraphBuilder builder;
    private List<GraphStep> steps;
    private String name;

    @Override
    public String name() {
        return name;
    }

    @Override
    public GraphBuilder builder() {
        return builder;
    }

    @Override
    public String input() {
        throw new UnsupportedOperationException("Multiple inputs for MergeStep");
    }

    @Override
    public List<String> inputs() {
        return steps.stream().map(GraphStep::name).collect(Collectors.toList());
    }

    @Override
    public boolean hasStep() {
        return false;
    }

    @Override
    public PipelineStep getStep() {
        throw new UnsupportedOperationException("MergeStep does not have a PipelineStep associated with it");
    }
}
