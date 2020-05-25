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

package ai.konduit.serving.pipeline.impl.pipeline.serde;

import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.graph.*;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.nd4j.common.base.Preconditions;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.core.TreeNode;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.deser.std.StdDeserializer;
import org.nd4j.shade.jackson.databind.node.NumericNode;
import org.nd4j.shade.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom JSON deserializer for deserializing {@link GraphStep}s
 *
 * @author Alex Black
 */
public class GraphStepDeserializer extends StdDeserializer<GraphStep> {


    protected GraphStepDeserializer() {
        super(GraphStep.class);
    }

    @Override
    public GraphStep deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {

        TreeNode tn = jp.readValueAsTree();
        TreeNode typeNode = tn.get(GraphConstants.TYPE_KEY);
        TreeNode inputNode = tn.get(GraphConstants.INPUT_KEY);



        String type = ((TextNode)typeNode).asText();
        JsonSubType st = ObjectMappers.findSubtypeByName(type);
        Preconditions.checkState(st != null, "No class found for mapping PipelineStep/GraphStep with type name \"%s\": " +
                "required module may not be on the classpath", st);

        if(PipelineStep.class.isAssignableFrom(st.getConfigInterface())){
            //Deserialize as PipelineStep, then wrap in a PipelineGraphStep
            PipelineStep ps = jp.getCodec().treeToValue(tn, PipelineStep.class);
            String input = ((TextNode)inputNode).asText();;
            return new PipelineGraphStep(null, ps, null, input);
        } else if(GraphStep.class.isAssignableFrom(st.getConfigInterface())){
            //Deserialize as GraphStep

            String input = null;
            List<String> inputs = null;
            if(inputNode.isArray()){
                int size = inputNode.size();
                inputs = new ArrayList<>(size);
                for( int i=0; i<size; i++ ){
                    inputs.add(((TextNode)inputNode.get(i)).asText());
                }
            } else {
                input = ((TextNode)inputNode).asText();
            }

            switch (type){
                case GraphConstants.GRAPH_MERGE_JSON_KEY:
                    return new MergeStep(null, inputs, null);   //TODO names
                case GraphConstants.GRAPH_ANY_JSON_KEY:
                    return new AnyStep(null, inputs, null);     //TODO names
                case GraphConstants.GRAPH_SWITCH_JSON_KEY:
                    TreeNode switchFnNode = tn.get("switchFn");
                    SwitchFn fn = jp.getCodec().treeToValue(switchFnNode, SwitchFn.class);
                    return new SwitchStep(null, null, input, fn);
                case GraphConstants.GRAPH_SWITCH_OUTPUT_JSON_KEY:
                    int idx = ((NumericNode)tn.get("outputNum")).intValue();
                    return new SwitchOutput(null, null, input, idx );  //TODO name
                default:
                    throw new UnsupportedOperationException("Unknown graph type JSON key: " + type);
            }
        } else {
            //Bad JSON?
            throw new IllegalStateException("Subtype \"" + type + "\" is neither a PipelineStep or GraphStep");
        }
    }
}
