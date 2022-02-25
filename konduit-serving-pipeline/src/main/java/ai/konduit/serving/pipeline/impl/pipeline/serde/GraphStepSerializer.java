/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.impl.pipeline.serde;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.*;
import ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchOutput;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.nd4j.shade.jackson.annotation.JsonPropertyOrder;
import org.nd4j.shade.jackson.annotation.JsonUnwrapped;
import org.nd4j.shade.jackson.core.JsonGenerator;
import org.nd4j.shade.jackson.databind.JsonSerializer;
import org.nd4j.shade.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A custom serializer for GraphSteps
 *
 * @author Alex Black
 */
public class GraphStepSerializer extends JsonSerializer<GraphStep> {

    @Override
    public void serialize(GraphStep gs, JsonGenerator jg, SerializerProvider sp) throws IOException {
        Map<Class<?>,String> names = ObjectMappers.getSubtypeNames();
        String stepJsonType = names.get(gs.getClass());
        Preconditions.checkState(gs instanceof PipelineGraphStep || stepJsonType != null, "No JSON name is known for GraphStep of type %s", gs);

        String name = gs.name();

        if(gs.hasStep()){
            //PipelineStep (StandardGraphStep) only
            PipelineStep s = gs.getStep();

            String type = names.get(s.getClass());
            String input = gs.input();
            StepSerializationHelper w = new StepSerializationHelper(type, input, s);
            jg.writeObject(w);
        } else {
            jg.writeStartObject(name);
            jg.writeFieldName(GraphConstants.TYPE_KEY);
            jg.writeString(stepJsonType);

            List<String> inputs = gs.inputs();
            jg.writeFieldName(GraphConstants.INPUT_KEY);
            if(inputs.size() == 1){
                jg.writeString(inputs.get(0));
            } else {
                jg.writeStartArray(inputs.size());
                for(String s : inputs){
                    jg.writeString(s);
                }
                jg.writeEndArray();
            }

            //Write all other fields
            //TODO maybe there's a better way... But GraphSteps don't really need to be user extensible or anything
            if(gs instanceof SwitchStep){
                SwitchStep ss = (SwitchStep)gs;
                SwitchFn fn = ss.switchFn();
                jg.writeFieldName("switchFn");
                jg.writeObject(fn);
            } else if(gs instanceof SwitchOutput){
                SwitchOutput so = (SwitchOutput)gs;
                jg.writeFieldName("outputNum");
                jg.writeNumber(so.outputNum());
            }
            //For AnyStep and MergeStep: No other fields to write (just need type and name)

            jg.writeEndObject();
        }
    }

    //Wrapper/helper class to inject "@type" and "@input" fields into the PipelineStep json
    @Data
    @AllArgsConstructor
    @JsonPropertyOrder({"@type", "@input"})
    protected static class StepSerializationHelper {
        @JsonProperty("@type")
        private String _typeAliasField_;
        @JsonProperty("@input")
        private String _inputAliasField_;
        @JsonUnwrapped
        private PipelineStep step;
    }
}
