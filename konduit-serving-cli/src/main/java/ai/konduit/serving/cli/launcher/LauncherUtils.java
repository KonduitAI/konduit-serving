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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static java.lang.System.setProperty;

/**
 * Common utility class for {@link KonduitServingLauncher} and its corresponding commands.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LauncherUtils {

    /**
     * This sets some of the common properties for vertx and logs. This will set the working directory
     * for vertx and channels the vertx related logs to the logback configuration that konduit-serving
     * utilizes.
     */
    public static void setCommonVertxProperties() {
        setProperty("vertx.cwd", DirectoryFetcher.getWorkingDir().getAbsolutePath()); // For setting the vertx working directory for runtime files.
        setProperty(CACHE_DIR_BASE_PROP_NAME, DirectoryFetcher.getWorkingDir().getAbsolutePath()); // For setting caching directory for vertx related optimizations.
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

            String[] outputSplits = IOUtils.toString(
                    new InputStreamReader(
                            new ProcessBuilder(cmd).start().getInputStream())).replace(System.lineSeparator(), "")
                    .trim().split(" ");

            String pid;
            if(ExecUtils.isWindows()) {
                pid = outputSplits[outputSplits.length -1].trim();
            } else {
                pid = outputSplits[0].trim();
            }

            return Integer.parseInt(pid);
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

        return output.trim()
                .replace(System.lineSeparator(), "")
                .matches("(.*)Dserving.id=" + applicationId);
    }
}
