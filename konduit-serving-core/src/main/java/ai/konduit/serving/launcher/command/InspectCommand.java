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

import ai.konduit.serving.launcher.LauncherUtils;
import ai.konduit.serving.settings.Fetcher;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.nio.charset.StandardCharsets;
import java.io.File;

@Slf4j
@Name("inspect")
@Summary("Inspect the details of a particular konduit server.")
@Description("Inspect the details of a particular konduit server given an id. To find a list of running servers and their details, use the 'list' command.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Prints the inference configuration of server with an id of 'inf_server':\n" +
        "$ konduit inspect inf_server\n" +
        "--------------")
public class InspectCommand extends DefaultCommand {

    String id;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void run() throws CLIException {
        if(LauncherUtils.isProcessExists(id)) {
            try {
                out.format("\nKonduit server with the id '%s' is initialized with the following configuration: \n\n%s\n\n", id,
                        FileUtils.readFileToString(new File(Fetcher.getServersDataDir(), LauncherUtils.getPidFromServerId(id) + ".data"), StandardCharsets.UTF_8));
            } catch (Exception exception) {
                log.error("Failed to read configuration file", exception);
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }

}
