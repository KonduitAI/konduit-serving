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

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.serde.GraphStepDeserializer;
import ai.konduit.serving.pipeline.impl.pipeline.serde.GraphStepSerializer;
import org.nd4j.shade.jackson.databind.annotation.JsonDeserialize;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * GraphStep represents one step within a GraphPipeline
 * Other than SwitchStep, all GraphSteps are either (single input, single output), or (multiple input, single output)
 *
 * @author Alex Black
 */
@JsonSerialize(using = GraphStepSerializer.class)
@JsonDeserialize(using = GraphStepDeserializer.class)
public interface GraphStep extends TextConfig {

    /**
     * @return Name of the graph step
     */
    String name();

    /**
     * Set the name of the GraphStep. Should not be used after graph has been created
     *
     * @param name Name to set
     */
    void name(String name);

    /**
     * @return The builder for this GraphStep. May be null after graph creation
     */
    GraphBuilder builder();

    /**
     * @return The number of inputs to this GraphStep
     */
    int numInputs();

    /**
     * @return The name of the input step, if one exists. If multiple inputs feed into this GraphStep, use {@link #inputs()} instead
     */
    String input();

    /**
     * @return Names of the inputs
     */
    List<String> inputs();

    /**
     * @return True if the GraphStep has a {@link PipelineStep} internally
     */
    boolean hasStep();

    /**
     * @return The {@link PipelineStep}, if one exists (according to {@link #hasStep()}
     */
    PipelineStep getStep();


    /**
     * Add a new GraphStep to the GraphBuilder/GraphPipeline, with the specified name, with data fed in from this step
     *
     * @param name Name of the new step
     * @param step New step to add
     * @return The added step as a GraphStep
     */
    default GraphStep then(String name, PipelineStep step) {
        GraphStep s = new PipelineGraphStep(builder(), step, name, this.name());
        ;
        builder().add(s);
        return s;
    }

    /**
     * Merge the output of this GraphStep with the specified GraphSteps<br>
     * This means that during execution, the output Data instance of this step (and the Data instances of other steps)
     * are combined together into a single Data instance
     *
     * @param name  Name for the new output step
     * @param steps Steps, the output of which should be merged with the output of this step
     * @return The GraphStep of the merged data
     */
    default GraphStep mergeWith(String name, GraphStep... steps) {
        List<String> allSteps = new ArrayList<>();
        allSteps.add(this.name());
        for (GraphStep g : steps) {
            allSteps.add(g.name());
        }

        MergeStep out = new MergeStep(builder(), allSteps, name);
        builder().add(out);
        return out;
    }

}
