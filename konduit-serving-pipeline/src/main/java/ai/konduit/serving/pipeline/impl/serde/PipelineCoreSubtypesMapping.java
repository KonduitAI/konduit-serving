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
package ai.konduit.serving.pipeline.impl.serde;

import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.*;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataIntSwitchFn;
import ai.konduit.serving.pipeline.impl.pipeline.graph.switchfn.DataStringSwitchFn;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.step.ml.ssd.SSDToBoundingBoxStep;

import java.util.ArrayList;
import java.util.List;

public class PipelineCoreSubtypesMapping implements JsonSubTypesMapping {


    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("LOGGING", LoggingPipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("SSD_TO_BBOX", SSDToBoundingBoxStep.class, PipelineStep.class));

        //Graph pipeline
        l.add(new JsonSubType("MERGE", MergeStep.class, GraphStep.class));
        l.add(new JsonSubType("ANY", AnyStep.class, GraphStep.class));
        l.add(new JsonSubType("SWITCH", SwitchStep.class, GraphStep.class));
        l.add(new JsonSubType("SWITCH_OUTPUT", SwitchOutput.class, GraphStep.class));

        //Graph pipeline switch functions
        l.add(new JsonSubType("INT_SWITCH", DataIntSwitchFn.class, SwitchFn.class));
        l.add(new JsonSubType("STRING_SWITCH", DataStringSwitchFn.class, SwitchFn.class));


        return l;
    }
}
