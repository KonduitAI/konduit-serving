package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.context.ProfilerConfig;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;
import ai.konduit.serving.pipeline.impl.util.CallbackPipelineStep;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineProfilerTest {
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testProfilingEvents() throws IOException {
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

        File logFile = testDir.newFile();
        ProfilerConfig profilerConfig = new ProfilerConfig();
        profilerConfig.setOutputFile(Paths.get(testDir.newFile().toURI()));
        pe.profilerConfig(profilerConfig);
        pe.exec(d);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append("\n");
        }
        System.out.println(sb.toString());
    }
}
