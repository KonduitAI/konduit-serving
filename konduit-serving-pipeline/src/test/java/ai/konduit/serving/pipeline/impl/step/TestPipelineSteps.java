package ai.konduit.serving.pipeline.impl.step;

import ai.konduit.serving.pipeline.api.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.util.CallbackPipelineStep;
import org.junit.Test;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TestPipelineSteps {

    @Test
    public void testLoggingPipeline(){

        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();
        Pipeline p = SequencePipeline.builder()
                .add(new CallbackPipelineStep(d -> count1.getAndIncrement()))
                .add(LoggingPipelineStep.builder().log(LoggingPipelineStep.Log.KEYS_AND_VALUES).logLevel(Level.INFO).build())
                .add(new CallbackPipelineStep(d -> count2.getAndIncrement()))
                .build();

        PipelineExecutor pe = p.executor();

        Data d = Data.singleton("someDouble", 1.0);
        d.put("someKey", "someValue");

        pe.exec(d);
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
        pe.exec(d);
        assertEquals(2, count1.get());
        assertEquals(2, count2.get());
    }

}
