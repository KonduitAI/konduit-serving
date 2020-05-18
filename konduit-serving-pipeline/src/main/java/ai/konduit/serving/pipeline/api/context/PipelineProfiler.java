package ai.konduit.serving.pipeline.api.context;

import lombok.extern.slf4j.Slf4j;
import org.nd4j.common.primitives.AtomicBoolean;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class PipelineProfiler implements Profiler {

    private Writer writer;
    private ObjectMapper json;
    private final Thread fileWritingThread;
    private final BlockingQueue<TraceEvent> writeQueue;
    private final AtomicBoolean writing = new AtomicBoolean(false);

    private long startTime;
    private long endTime;

    public PipelineProfiler(ProfilerConfig profilerConfig) {
        try {
            this.writer = new BufferedWriter(new FileWriter(profilerConfig.getOutputFile().toString(), false));
            this.writer.write("[");     //JSON array open (array close is optional for Chrome profiler format)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.json = new ObjectMapper();

        //Set up a queue so file access doesn't add latency to the execution thread
        writeQueue =
                new LinkedBlockingDeque<>();
        fileWritingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runHelper();
                } catch (Throwable t) {
                    log.error("Error when attempting to write results to file", t);
                }
            }

            public void runHelper() throws Exception {
                while (true) {
                    TraceEvent te = writeQueue.take();    //Blocking
                    writing.set(true);
                    try {
                        String j = json.writeValueAsString(te);
                        writer.append(j);
                        writer.append(",\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        writing.set(false);
                    }
                }
            }
        });
        fileWritingThread.setDaemon(true);
        fileWritingThread.start();
    }

    @Override
    public boolean profilerEnabled() {
        return true;
    }

    @Override
    public void eventStart(String key) {

        startTime = System.currentTimeMillis();

        TraceEvent event = TraceEvent.builder()
                .name(key)
                .timeStampStart(startTime)
                .type(TraceEvent.EventType.START)
                .build();

        writeQueue.add(event);
    }

    @Override
    public void eventEnd(String key) {
        endTime = System.currentTimeMillis();

        TraceEvent event = TraceEvent.builder()
                .name(key)
                .timeStampEnd(endTime)
                .type(TraceEvent.EventType.END)
                .build();

        writeQueue.add(event);
    }
}
