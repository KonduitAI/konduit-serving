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

import ai.konduit.serving.util.LogUtils;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.io.File;

@Slf4j
@Name("logs")
@Summary("View the logs of a particular konduit server")
@Description("View the logs of a particular konduit server given an id.")
public class LogsCommand extends DefaultCommand {

    String id;
    boolean follow;

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

    @Override
    public void run() throws CLIException {
        try {
            File logsFile = Paths.get(LogUtils.getLogsDir(), id + ".log").toFile();

            if (follow) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logsFile)));

                String line;
                while (true) {
                    line = reader.readLine();
                    if (line == null) {
                        Thread.sleep(100);
                    } else {
                        out.println(line);
                    }
                }
            } else {
                out.println(FileUtils.readFileToString(logsFile, StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            log.error("Failed to read logs. Reason: {}", exception.getMessage());
        }
    }
}