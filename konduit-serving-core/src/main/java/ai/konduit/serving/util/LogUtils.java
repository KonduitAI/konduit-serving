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

package ai.konduit.serving.util;

import ai.konduit.serving.settings.Fetcher;
import ai.konduit.serving.settings.constants.Constants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.LoggerFactory;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class LogUtils {
    /**
     * Gets the file where the logs are.
     * @return the logs file.
     */
    public static File getEndpointLogsFile() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) rootLogger.getAppender("FILE");

        if(fileAppender != null) {
            return new File(fileAppender.getFile());
        } else {
            return null;
        }
    }

    /**
     * Reads the last n lines from a file
     * @param file file where the data to be read is.
     * @param numOfLastLinesToRead the number of last lines to read
     * @return read lines
     */
    public static String readLastLines(File file, int numOfLastLinesToRead) throws IOException {
        List<String> result = new ArrayList<>();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && result.size() < numOfLastLinesToRead) {
                result.add(line);
            }
        } catch (IOException e) {
            log.error("Error while reading log file", e);
            throw e;
        }

        Collections.reverse(result);
        return String.join(System.lineSeparator(), result);
    }

    /**
     * Sets the file appender with the name of "FILE" if needed. If it's already been setup,
     * it would be ignored.
     */
    public static void setFileAppenderIfNeeded() throws Exception {
        File previousLogsFile = getEndpointLogsFile();

        File newLogsFile = new File(Fetcher.getEndpointLogsDir(), Constants.MAIN_ENDPOINT_LOGS_FILE);

        if(!newLogsFile.equals(previousLogsFile)) {
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)
                    org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

            LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();

            if(previousLogsFile != null) {
                rootLogger.detachAppender("FILE");
            }

            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setName("FILE");
            fileAppender.setFile(newLogsFile.getAbsolutePath());
            fileAppender.setContext(context);

            PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
            patternLayoutEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            patternLayoutEncoder.setContext(context);
            patternLayoutEncoder.start();

            fileAppender.setEncoder(patternLayoutEncoder);
            fileAppender.start();

            rootLogger.addAppender(fileAppender);
        }
    }

    /**
     * Sets the appenders for command line.
     */
    public static void setAppendersForCommandLine() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();

        rootLogger.detachAndStopAllAppenders();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();

        PatternLayoutEncoder consolePatternLayoutEncoder = new PatternLayoutEncoder();
        consolePatternLayoutEncoder.setPattern("%msg%n");
        consolePatternLayoutEncoder.setContext(context);
        consolePatternLayoutEncoder.start();

        consoleAppender.setEncoder(consolePatternLayoutEncoder);
        consoleAppender.start();

        rootLogger.addAppender(consoleAppender);

        ((Logger) LoggerFactory.getLogger("uk.org.lidalia")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org.nd4j")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("org")).setLevel(Level.ERROR);
        ((Logger) LoggerFactory.getLogger("io")).setLevel(Level.ERROR);

        ((Logger) LoggerFactory.getLogger("ai")).setLevel(Level.INFO);
    }

    /**
     * Sets the file appender with the name of "FILE" if needed. If it's already been setup,
     * it would be ignored.
     */
    public static void setAppendersForRunCommand(String serverId) throws Exception {
        String logFilePath = new File(Fetcher.getCommandLogsDir(), serverId + ".log").getAbsolutePath();

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        rootLogger.detachAndStopAllAppenders();

        ((Logger) LoggerFactory.getLogger("uk.org.lidalia")).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.nd4j")).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org")).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("io")).setLevel(Level.INFO);

        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setName("FILE");
        fileAppender.setFile(logFilePath);
        fileAppender.setAppend(false);
        fileAppender.setContext(context);

        PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
        patternLayoutEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        patternLayoutEncoder.setContext(context);
        patternLayoutEncoder.start();

        fileAppender.setEncoder(patternLayoutEncoder);
        fileAppender.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setName("CONSOLE");
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(patternLayoutEncoder);
        consoleAppender.start();

        rootLogger.addAppender(fileAppender);
        rootLogger.addAppender(consoleAppender);

        log.info("Logging file at: {}", logFilePath);
    }

    /**
     * Finds the log file and sends the output as a String
     * @param numOfLastLinesToRead Number of lines to read from the last few lines. If it's less than 1 then it will
     *                             return all the log file data.
     * @return current jvm process logs for konduit-serving.
     */
    public static String getLogs(int numOfLastLinesToRead) throws IOException {
        File logsFile = getEndpointLogsFile();

        if(logsFile == null || !logsFile.exists()) return "";

        if(numOfLastLinesToRead > 0) {
            return readLastLines(logsFile, numOfLastLinesToRead);
        } else {
            try {
                return FileUtils.readFileToString(logsFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Error reading logs file: ", e);
                throw e;
            }
        }
    }

    public static File getZippedLogs() throws Exception {
        File zippedFile = new File(Fetcher.getEndpointLogsDir(), "logs.zip");

        try (BufferedOutputStream archiveStream = new BufferedOutputStream(new FileOutputStream(zippedFile))) {
            try (ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream)) {
                File logsFile = getEndpointLogsFile();

                if(logsFile != null) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(logsFile.getName());
                    archive.putArchiveEntry(entry);

                    try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(logsFile))) {
                        IOUtils.copy(input, archive);
                        archive.closeArchiveEntry();
                        archive.finish();
                    }
                } else {
                    throw new FileNotFoundException("No logs file found!");
                }
            }
        }

        return zippedFile;
    }
}
