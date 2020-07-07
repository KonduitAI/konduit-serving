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

package ai.konduit.serving.cli.launcher.command;

import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.konduit.serving.cli.launcher.LauncherUtils.getPidFromServerId;
import static ai.konduit.serving.cli.launcher.LauncherUtils.isProcessExists;

@Name(value = "stop", priority = 1)
@Summary("Stop a running konduit server")
@Description("This command stops a konduit server started with the `serve` command. The command requires the " +
        "serving id as argument. Use the `list` command to get the list of running konduit servers.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Stops the server with an id of 'inf_server':\n" +
        "$ konduit stop inf_server\n" +
        "--------------")
@Slf4j
public class StopCommand extends DefaultCommand {

    private String id;

    /**
     * Whether or not we are in redeploy mode. In redeploy mode, do not exit the VM.
     */
    private boolean redeploy;

    private static final Pattern PS = Pattern.compile("([0-9]+)\\s.*-Dserving.id=.*");

    /**
     * As the {@code stop} command takes only a single argument, it's the application id.
     *
     * @param id the id.
     */
    @Argument(index = 0, argName = "serving.id", required = false)
    @Description("The konduit server id")
    public void setApplicationId(String id) {
        this.id = id;
    }

    @Option(longName = "redeploy", flag = true)
    @Hidden
    public void setRedeploy(boolean redeploy) {
        this.redeploy = redeploy;
    }

    /**
     * Stops a running konduit server launched with the `serve` command.
     */
    @Override
    public void run() {
        if (id == null) {
            out.println("Application id not specified. See `stop --help` for more info.");
            executionContext.execute("list");
            return;
        }

        if(!isProcessExists(id)) {
            out.println(String.format("No konduit server exists with an id: '%s'.", id));
            return;
        } else {
            // Cleaning up current server data file
            File serverDataFile = new File(DirectoryFetcher.getServersDataDir(), getPidFromServerId(id) + ".data");
            try {
                FileUtils.forceDelete(serverDataFile);
            } catch (IOException exception) {
                if(!(exception instanceof FileNotFoundException)) { // Ignoring FileNotFoundException since the file won't need to be deleted then.
                    log.error("Unable to delete server data file at: {}", serverDataFile.getAbsolutePath(), exception);
                }
            }
        }

        out.println("Stopping konduit server '" + id + "'");
        if (ExecUtils.isWindows()) {
            terminateWindowsApplication();
        } else {
            terminateLinuxApplication();
        }
    }

    private void terminateLinuxApplication() {
        String pid = pid();
        if (pid == null) {
            out.println("Cannot find process for application using the id '" + id + "'.");
            if (!redeploy) {
                ExecUtils.exitBecauseOfProcessIssue();
            }
            return;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("kill");
        cmd.add(pid);
        try {
            int result = new ProcessBuilder(cmd).start().waitFor();
            out.println("Application '" + id + "' terminated with status " + result);
            if (!redeploy && result != 0) {
                // We leave the application using the same exit code.
                ExecUtils.exit(result);
            }
        } catch (Exception e) {
            out.println("Failed to stop application '" + id + "'");
            e.printStackTrace(out);
            if (!redeploy) {
                ExecUtils.exitBecauseOfProcessIssue();
            }
        }
    }

    private void terminateWindowsApplication() {
        // Use wmic.
        List<String> cmd = Arrays.asList(
                "WMIC",
                "PROCESS",
                "WHERE",
                "\"CommandLine like '%serving.id=" + id + "' and name!='wmic.exe'\"",
                "CALL",
                "TERMINATE"
        );

        try {
            final Process process = new ProcessBuilder(cmd).start();

            int result = process.waitFor();
            out.println("Application '" + id + "' terminated with status " + result);
            if (!redeploy && result != 0) {
                // We leave the application using the same exit code.
                ExecUtils.exit(result);
            }
        } catch (Exception e) {
            out.println("Failed to stop application '" + id + "'");
            e.printStackTrace(out);
            if (!redeploy) {
                ExecUtils.exitBecauseOfProcessIssue();
            }
        }
    }

    private String pid() {
        try {
            final Process process = new ProcessBuilder(Arrays.asList("sh", "-c", "ps ax | grep \"Dserving.id=" + id + "$\"")).start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                final Matcher matcher = PS.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            process.waitFor();
            reader.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace(out);
        } catch (Exception e) {
            e.printStackTrace(out);
            out.println("Failed to get process ID.");
        }
        return null;
    }
}
