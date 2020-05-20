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
package ai.konduit.serving.pipeline.api.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.nd4j.common.io.StringUtils;
import org.nd4j.common.primitives.AtomicBoolean;
import org.nd4j.shade.jackson.databind.ObjectMapper;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
    private final long pid;
    private final long tid;

    @Getter
    private boolean logActive;
    private ProfilerConfig profilerConfig;
    private Path currentLog;

    private long getProcessId() {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return 0;
        }

        try {
            return Long.parseLong(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            // ignore
        }
        return 0;
    }

    private void fileSizeGuard() throws IOException {
        if ((profilerConfig.getSplitSize() > 0) &&
            (Files.size(profilerConfig.getOutputFile()) > profilerConfig.getSplitSize())) {
            String baseName = FilenameUtils.removeExtension(currentLog.getFileName().toString());
            int num = 1;
            String counted = org.apache.commons.lang3.StringUtils.EMPTY;
            String[] parts = baseName.split("_");
            if (parts.length > 1) {
                counted = parts[0] + "_" + (Integer.parseInt(parts[1]) + 1);
            }
            else {
                counted = baseName + "_" + num;
            }
            Path nextFile = Paths.get(currentLog.getParent() + FileSystems.getDefault().getSeparator() +
                    counted + ".json");
            Files.createFile(nextFile);
            try {
                this.writer = new BufferedWriter(new FileWriter(nextFile.toString(), true));
                this.writer.write("[");     //JSON array open (array close is optional for Chrome profiler format)
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.currentLog = nextFile;
        }
    }

    public PipelineProfiler(ProfilerConfig profilerConfig) {
        this.profilerConfig = profilerConfig;
        this.currentLog = profilerConfig.getOutputFile();
        try {
            this.writer = new BufferedWriter(new FileWriter(this.currentLog.toString(), true));
            this.writer.write("[");     //JSON array open (array close is optional for Chrome profiler format)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.json = new ObjectMapper();
        this.pid = getProcessId();
        this.tid = Thread.currentThread().getId();

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
    public void eventStart(String key) throws IOException {
        fileSizeGuard();
        logActive = true;
        startTime = System.nanoTime() / 1000;

        TraceEvent event = TraceEvent.builder()
                .name(key)
                .cat(Collections.singletonList("Step"))
                .ts(startTime)
                .ph(TraceEvent.EventType.START)
                .pid(this.pid)
                .tid(this.tid)
                .build();

        writeQueue.add(event);
    }

    @Override
    public void eventEnd(String key) throws IOException {
        if (logActive) {
            while ((!writeQueue.isEmpty() || writing.get()) && fileWritingThread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        logActive = false;
        fileSizeGuard();
        endTime = System.nanoTime() / 1000;

        TraceEvent event = TraceEvent.builder()
                .name(key)
                .cat(Collections.singletonList("Step"))
                .ts(endTime)
                .ph(TraceEvent.EventType.END)
                .pid(this.pid)
                .tid(this.tid)
                .build();

        writeQueue.add(event);
    }

    public TraceEvent[] readEvents(File file) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        content = StringUtils.trimTrailingWhitespace(content);
        if (content.endsWith(","))
            content = content.substring(0, content.length()-1) + "]";
        if (StringUtils.isEmpty(content))
            return new TraceEvent[0];
        TraceEvent[] events = json.readValue(content, TraceEvent[].class);
        return events;
    }
}
