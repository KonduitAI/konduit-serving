package ai.konduit.serving;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunnerFactory;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.registry.PipelineRegistry;
import org.junit.Assert;
import org.junit.Test;

public class PythonStepTest {

    @Test
    public void testPythonStepBasic(){
        String code = "def setup():pass\ndef run(data):\n\tdata['x'] += 1\n\treturn data";
        PythonStep step = PythonStep.builder().code(code).setupMethod("setup").runMethod("run").build();
        Pipeline pipeline = SequencePipeline.builder().add(step).build();
        Data input = new JData();
        input.put("x", 5);
        Data output = pipeline.executor().exec(input);
        Assert.assertEquals(output.getLong("x"), 6L);
    }
}
