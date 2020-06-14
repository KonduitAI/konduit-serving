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

import ai.konduit.serving.cli.launcher.LauncherUtils;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static java.lang.System.out;

@Slf4j
@Name("logs")
@Summary("View the logs of a particular konduit server")
@Description("View the logs of a particular konduit server given an id.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Outputs the log file contents of server with an id of 'inf_server':\n" +
        "$ konduit logs inf_server\n\n" +
        "- Outputs and tail the log file contents of server with an id of 'inf_server':\n" +
        "$ konduit logs inf_server -f\n\n" +
        "- Outputs and tail the log file contents of server with an id of 'inf_server' \n" +
        "  from the last 10 lines:\n" +
        "$ konduit logs inf_server -l 10 -f \n" +
        "--------------")
public class LogsCommand extends DefaultCommand {

    private String id;
    private boolean follow;
    private int lines = 10;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Option(longName = "follow", shortName = "f", flag = true)
    @Description("Follow the logs output.")
    public void setFollow(boolean follow) {
        this.follow = follow;
    }

    @Option(longName = "lines", shortName = "l")
    @Description("Sets the number of lines to be printed. Default is '10'. Use -1 for outputting everything.")
    public void setLines(String lines) {
        try {
            this.lines = Integer.parseInt(lines);
            if(this.lines != -1 && this.lines < 1) {
                System.out.format("Number of lines to be printed should be greater than 0. " +
                        "Current it is %s%n", lines);
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.format("Unable to parse number of lines (%s) to a number%n", lines);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            File logsFile = new File(DirectoryFetcher.getCommandLogsDir(), id + ".log");

            if (follow) {
                readAndTail(logsFile, lines);
            } else {
                if(lines == -1) {
                    out.println(FileUtils.readFileToString(logsFile, StandardCharsets.UTF_8));
                } else {
                    out.println(LauncherUtils.readLastLines(logsFile, lines));
                }
            }
        } catch (Exception exception) {
            out.println("Failed to read logs:");
            exception.printStackTrace(out);
        }
    }

    private void readAndTail(File logsFile, int fromNumberOfLines) throws IOException {
        new Tailer(logsFile, StandardCharsets.UTF_8, new TailerListenerAdapter() {
            @SneakyThrows
            @Override
            public void init(Tailer tailer) {
                super.init(tailer);
                if(fromNumberOfLines != -1) {
                    out.println(LauncherUtils.readLastLines(logsFile, fromNumberOfLines));
                }
            }

            @Override
            public void handle(String line) {
                out.println(line);
            }

            @Override
            public void handle(Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }, 100, fromNumberOfLines != -1, false, 4096).run();
    }
}
