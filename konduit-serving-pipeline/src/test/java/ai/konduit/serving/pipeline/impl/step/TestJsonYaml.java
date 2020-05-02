package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestJsonYaml {

    @Test
    public void testJsonYamlSimple(){
        Pipeline p = SequencePipeline.builder()
                .add(LoggingPipelineStep.builder().build())
                .build();

        String json = p.toJson();
        Pipeline pJson = Pipeline.fromJson(json);
        assertEquals(p, pJson);


        String yaml = p.toYaml();
        Pipeline pYaml = Pipeline.fromYaml(yaml);
        assertEquals(p, pYaml);
    }

    @Test
    public void testJsonYamlSimpleGraph(){
        Map<String, PipelineStep> m = Collections.singletonMap("logging", LoggingPipelineStep.builder().build());
        Pipeline p = new GraphPipeline(m);

        String json = p.toJson();
        Pipeline pJson = Pipeline.fromJson(json);
        assertEquals(p, pJson);


        String yaml = p.toYaml();
        Pipeline pYaml = Pipeline.fromYaml(yaml);
        assertEquals(p, pYaml);
    }


}
