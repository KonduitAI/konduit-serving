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
package ai.konduit.serving.pipeline.impl.pipeline.serde;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import org.nd4j.shade.jackson.core.JsonParseException;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.core.TreeNode;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.deser.std.StdDeserializer;
import org.nd4j.shade.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PipelineDeserializer extends StdDeserializer<Pipeline> {

    protected PipelineDeserializer() {
        super(Pipeline.class);
    }

    @Override
    public Pipeline deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        TreeNode tn = jp.readValueAsTree();
        TreeNode n = tn.get("steps");
        String id = null;
        if(tn.get("id") != null){
            id = ((TextNode)tn.get("id")).asText();
        }

        Trigger asyncTrigger = null;        //If present: it's an async pipeline
        if(tn.get(Data.RESERVED_KEY_ASYNC_TRIGGER) != null){
            TreeNode triggerNode = tn.get(Data.RESERVED_KEY_ASYNC_TRIGGER);
            asyncTrigger = jp.getCodec().treeToValue(triggerNode, Trigger.class);
        }

        Pipeline p;
        if(n.isArray()){
            PipelineStep[] steps = jp.getCodec().treeToValue(n, PipelineStep[].class);
            p = new SequencePipeline(Arrays.asList(steps), id);
        } else if(n.isObject()){
            Map<String, GraphStep> map = new LinkedHashMap<>();

            Iterator<String> f = n.fieldNames();
            while(f.hasNext()) {
                String s = f.next();
                TreeNode pn = n.get(s);
                GraphStep step = jp.getCodec().treeToValue(pn, GraphStep.class);
                step.name(s);
                map.put(s, step);
            }

            String outputStep = ((TextNode)tn.get("outputStep")).asText();

            p = new GraphPipeline(map, outputStep, id);
        } else {
            throw new JsonParseException(jp, "Unable to deserialize Pipeline: Invalid JSON/YAML? Pipeline is neither a SequencePipeline or a GraphPipeline");
        }

        if(asyncTrigger != null ){
            return new AsyncPipeline(p, asyncTrigger);
        }

        return p;
    }
}
