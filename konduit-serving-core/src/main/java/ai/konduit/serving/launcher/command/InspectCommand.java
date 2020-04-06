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

package ai.konduit.serving.launcher.command;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Name("inspect")
@Summary("Inspect the details of a particular konduit server.")
@Description("Inspect the details of a particular konduit server given an id.")
public class InspectCommand extends DefaultCommand {

    String id;

    @Argument(index = 0, argName = "<server-id>")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void run() throws CLIException {
        if(ServeCommand.isProcessExists(id)) {
            try {
                out.format("\nKonduit server with the id '%s' is initialized with the following configuration: \n\n%s\n\n", id,
                        FileUtils.readFileToString(Paths.get(System.getProperty("user.home"), ".konduit-serving", "servers", getPidFromId(id) + ".data").toFile(), StandardCharsets.UTF_8));
            } catch (Exception exception) {
                log.error("Failed to read configuration file", exception);
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }

    public static int getPidFromId(String serverId) {
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

            return Integer.valueOf(pid);
        } catch (Exception exception) {
            log.error("Failed to fetch pid from server id", exception);
            System.exit(1);
            return -1;
        }
    }
}
