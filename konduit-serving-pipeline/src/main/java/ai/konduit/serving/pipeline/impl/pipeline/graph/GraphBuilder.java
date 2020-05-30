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

import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import org.nd4j.common.base.Preconditions;

import java.util.*;

/**
 * A utility class for building {@link GraphPipeline} instances.<br>
 * Usage:
 * <pre>
 * {@code
 * GraphBuilder b = new GraphBuilder();
 * GraphStep input = b.input();
 * GraphStep output = input.then("myStep", ...);
 * Pipeline p = b.build(output);
 * }}</pre>
 *
 * @author Alex Black
 */
public class GraphBuilder {

    private List<GraphStep> steps = new ArrayList<>();
    private final GraphStep input = new Input(this);
    private String id;

    public GraphStep input() {
        return input;
    }

    public GraphBuilder id(String id){
        this.id = id;
        return this;
    }

    public GraphStep[] switchOp(String name, SwitchFn fn, GraphStep step) {
        int nOut = fn.numOutputs();

        SwitchStep swStep = new SwitchStep(this, name, step.name(), fn);
        add(swStep);

        GraphStep[] out = new GraphStep[nOut];
        for (int i = 0; i < nOut; i++) {
            String oName = name + "_" + i;
            out[i] = new SwitchOutput(this, oName, name, i);
            add(out[i]);
        }
        return out;
    }

    public GraphStep any(String name, GraphStep... steps) {
        List<String> l = new ArrayList<>();
        for (GraphStep g : steps) {
            l.add(g.name());
        }
        GraphStep g = new AnyStep(this, l, name);
        add(g);
        return g;
    }

    //Package private
    void add(GraphStep step) {
        Preconditions.checkState(!hasStep(step.name()), "Graph pipeline already has a step with name \"%s\"", step.name());
        steps.add(step);
    }

    //Package private
    boolean hasStep(String name) {
        for (GraphStep g : steps) {
            if (name.equals(g.name()))
                return true;
        }
        return false;
    }

    public GraphPipeline build(GraphStep outputStep) {
        Map<String, GraphStep> m = new HashMap<>();
        for (GraphStep g : steps) {
            m.put(g.name(), g);
        }
        m.put(outputStep.name(), outputStep);
        return new GraphPipeline(m, outputStep.name(), id);
    }

}
