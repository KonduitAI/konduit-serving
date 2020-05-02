package ai.konduit.serving.pipeline.impl.pipeline.serde;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.nd4j.shade.jackson.core.JsonParseException;
import org.nd4j.shade.jackson.core.JsonParser;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.core.TreeNode;
import org.nd4j.shade.jackson.core.type.TypeReference;
import org.nd4j.shade.jackson.databind.DeserializationContext;
import org.nd4j.shade.jackson.databind.JsonNode;
import org.nd4j.shade.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

public class PipelineDeserializer extends StdDeserializer<Pipeline> {

    protected PipelineDeserializer() {
        super(Pipeline.class);
    }

    @Override
    public Pipeline deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        TreeNode tn = jp.readValueAsTree();
        TreeNode n = tn.get("steps");

        if(n.isArray()){
            PipelineStep[] steps = jp.getCodec().treeToValue(n, PipelineStep[].class);
            return new SequencePipeline(Arrays.asList(steps));
        } else if(n.isObject()){
            Map<String,PipelineStep> map = new LinkedHashMap<>();

            Iterator<String> f = n.fieldNames();
            while(f.hasNext()) {
                String s = f.next();
                TreeNode pn = n.get(s);
                PipelineStep ps = jp.getCodec().treeToValue(pn, PipelineStep.class);
                map.put(s, ps);
            }

            return new GraphPipeline(map);
        } else {
            throw new JsonParseException(jp, "Unable to deserialize Pipeline: Invalid JSON/YAML? Pipeline is neither a SequencePipeline or a GraphPipeline");
        }

    }
}
