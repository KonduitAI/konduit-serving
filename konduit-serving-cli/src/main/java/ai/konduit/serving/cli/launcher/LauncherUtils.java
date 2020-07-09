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

package ai.konduit.serving.cli.launcher;

import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static java.lang.System.setProperty;

/**
 * Common utility class for {@link KonduitServingLauncher} and its corresponding commands.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LauncherUtils {

    public static final int SECONDS_IN_DAY = 86400;

    /**
     * This sets some of the common properties for vertx and logs. This will set the working directory
     * for vertx and channels the vertx related logs to the logback configuration that konduit-serving
     * utilizes.
     */
    public static void setCommonVertxProperties() {
        setProperty("vertx.cwd", DirectoryFetcher.getVertxDir().getAbsolutePath()); // For setting the vertx working directory for runtime files.
        setProperty(CACHE_DIR_BASE_PROP_NAME, DirectoryFetcher.getVertxDir().getAbsolutePath()); // For setting caching directory for vertx related optimizations.
    }

    /**
     * Gets the process id of the konduit server process given its application id.
     * @param serverId application id of the konduit server application.
     * @return process id of the konduit server process.
     */
    public static int getPidFromServerId(String serverId) {
        List<String> cmd = new ArrayList<>();
        try {
            if (ExecUtils.isWindows()) {
                cmd.add("WMIC");
                cmd.add("PROCESS");
                cmd.add("WHERE");
                cmd.add("\"CommandLine like '%serving.id=" + serverId + "' and name!='wmic.exe'\"");
                cmd.add("GET");
                cmd.add("CommandLine,ProcessId");
            } else {
                cmd.add("sh");
                cmd.add("-c");
                cmd.add("ps ax | grep \"serving.id=" + serverId + "$\"");
            }

            return Integer.parseInt(extractPidFromLine(IOUtils.toString(new InputStreamReader(
                    new ProcessBuilder(cmd).start().getInputStream())).replace(System.lineSeparator(), "")));
        } catch (Exception exception) {
            log.error("Failed to fetch pid from server id", exception);
            System.exit(1);
            return -1;
        }
    }

    /**
     * Parses the command line that was used to start the konduit server and extracts the
     * application id (name) of the server.
     * @param line command line that was used to start the konduit server.
     * @return application id of the konduit server.
     */
    public static String extractPidFromLine(String line) {
        String[] splits = line.trim().split(" ");

        if(ExecUtils.isWindows()) {
            return splits[splits.length -1].trim();
        } else {
            return splits[0].trim();
        }
    }

    /**
     * Checks if there is a konduit server running with the given application id.
     * @param applicationId application id of the konduit server.
     * @return true if the server process exists, false otherwise.
     */
    public static boolean isProcessExists(String applicationId) {
        List<String> args;

        if(SystemUtils.IS_OS_WINDOWS) {
            args = Arrays.asList("WMIC", "PROCESS", "WHERE", "\"CommandLine like '%serving.id=" + applicationId + "' and name!='wmic.exe'\"", "GET", "CommandLine", "/VALUE");
        } else {
            args = Arrays.asList("sh", "-c", "ps ax | grep \"Dserving.id=" + applicationId + "$\"");
        }

        String output = "";
        try {
            Process process = new ProcessBuilder(args).start();
            output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            log.error("An error occurred while checking for existing processes:", exception);
            System.exit(1);
        }

        return output.trim().endsWith("Dserving.id=" + applicationId);
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
     * Cleans up the server data files daily.
     */
    public static void cleanServerDataFilesOnceADay() {
        Date timeNow = Date.from(Instant.now());

        File lastCheckedFile = new File(DirectoryFetcher.getServersDataDir(), "lastChecked");
        Date lastChecked = timeNow;
        boolean firstTime = false;
        if(lastCheckedFile.exists()) {
            try {
                lastChecked = DateFormat.getInstance().parse(FileUtils.readFileToString(lastCheckedFile, StandardCharsets.UTF_8));
            } catch (IOException | ParseException exception) {
                log.error("Unable to identify last server data file cleanup check", exception);
                return; // Stop cleaning up
            }
        } else {
            firstTime = true;
        }

        if(timeNow.toInstant().getEpochSecond() - lastChecked.toInstant().getEpochSecond() > SECONDS_IN_DAY || firstTime) {
            cleanServerDataFiles();
        }

        try {
            FileUtils.writeStringToFile(lastCheckedFile, DateFormat.getInstance().format(timeNow), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.error("Unable to set last checked clean up time at: {}", lastCheckedFile.getAbsolutePath(), exception);
        }
    }

    /**
     * Cleans extra server files with the name of <pid>.data at {@link DirectoryFetcher#getServersDataDir} which
     * doesn't have a process associated with it.
     */
    public static void cleanServerDataFiles() {
        for(File file : FileUtils.listFiles(DirectoryFetcher.getServersDataDir(), new RegexFileFilter("\\d+.data",
                IOCase.INSENSITIVE), null)) {

            String pid = file.getName().split("\\.")[0];
            boolean deleting = false;
            try {
                if (!LauncherUtils.isKonduitServer(pid)) {
                    deleting = true;
                    FileUtils.forceDelete(file);
                }
            } catch (IOException exception) {
                if(deleting) {
                    log.error("Unable to delete server data file at: {}", file.getAbsolutePath(), exception);
                } else {
                    log.error("Unable to identify a konduit serving process on the given id: {}", pid);
                }
            }
        }
    }

    /**
     * Check if the process identified by the given pid is a konduit serving process
     * @param pid Process pid
     * @return true if it's a konduit serving process otherwise false
     */
    private static boolean isKonduitServer(String pid) throws IOException {
        List<String> args;

        if(SystemUtils.IS_OS_WINDOWS) {
            args = Arrays.asList("WMIC", "PROCESS", "WHERE", "ProcessId=" + pid, "GET", "CommandLine", "/VALUE");
        } else {
            args = Arrays.asList("sh", "-c", "ps ax | grep \"\\b+" + pid + "\\b+\"");
        }

        Process process = new ProcessBuilder(args).start();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);

        return output.contains("Dserving.id=");
    }
}
