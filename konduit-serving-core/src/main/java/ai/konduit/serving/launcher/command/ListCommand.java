/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.launcher.command;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.launcher.LauncherUtils;
import ai.konduit.serving.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Name(value = "list", priority = 1)
@Summary("Lists the running konduit servers.")
@Description("List all konduit servers launched through the `serve` command.")
public class ListCommand extends DefaultCommand {

    private static final Pattern PS = Pattern.compile("-Dserving.id=(.*)\\s*");
    private static final Pattern ST = Pattern.compile("\\s+-s\\s+(.*)\\s*|\\s+--service\\s+(.*)\\s*");

    // Note about stack traces - the stack trace are printed on the stream passed to the command.

    /**
     * Executes the {@code list} command.
     */
    @Override
    public void run() {
        out.println("\nListing konduit servers...\n");
        List<String> cmd = new ArrayList<>();
        try {
            if (ExecUtils.isWindows()) {
                cmd.add("WMIC");
                cmd.add("PROCESS");
                cmd.add("WHERE");
                cmd.add("\"CommandLine like '%serving.id%' and name!='wmic.exe'\"");
                cmd.add("GET");
                cmd.add("CommandLine,ProcessId");
            } else {
                cmd.add("sh");
                cmd.add("-c");
                cmd.add("ps ax | grep \"serving.id=\"");
            }

            dumpFoundVertxApplications(cmd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(out);
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    private void dumpFoundVertxApplications(List<String> cmd) throws IOException, InterruptedException {
        String printFormat = " %1$-3s | %2$-30s | %3$-10s | %4$-20s | %5$-7s | %6$-10s \n";

        boolean none = true;
        final Process process = new ProcessBuilder(cmd).start();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        int index = 0;
        while ((line = reader.readLine()) != null) {
            final Matcher matcher = PS.matcher(line);
            if (matcher.find()) {
                index++;
                if(none) {
                    out.format(printFormat, "#", "ID", "TYPE", "URL", "PID", "STATUS");
                }

                String id = matcher.group(1).trim().split(" ")[0];
                printServerDetails(index, printFormat, id, line);
                none = false;
            }
        }

        if(index > 0) {
            out.println();
        }

        process.waitFor();
        reader.close();
        if (none) {
            out.println("No konduit servers found.");
        }
    }

    private void printServerDetails(int index, String printFormat, String id, String line) {
        String pid = LauncherUtils.extractPidFromLine(line);
        String configuration;
        String hostAndPort = "waiting...";
        String status = "starting";

        try {
            configuration = FileUtils.readFileToString(new File(DirectoryFetcher.getServersDataDir(), pid + ".data"), StandardCharsets.UTF_8);
            ServingConfig servingConfig = InferenceConfiguration.fromJson(configuration).getServingConfig();
            hostAndPort = String.format("%s:%s", servingConfig.getListenHost(), servingConfig.getHttpPort());
            status = "started";
        } catch (IOException exception) {
            log.debug("Error occurred while reading server configuration file\n", exception);
        }

        out.format(printFormat, index, id, getServiceType(line), hostAndPort, pid, status);
    }

    private String getServiceType(String line) {
        Matcher matcher = ST.matcher(line);
        if(matcher.find()) {
            String output = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return output.trim().split(" ")[0];
        } else {
            return "inference"; // Default service assumed.
        }
    }
}
