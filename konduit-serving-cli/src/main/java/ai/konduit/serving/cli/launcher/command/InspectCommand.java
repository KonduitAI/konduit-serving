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
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Name("inspect")
@Summary("Inspect the details of a particular konduit server.")
@Description("Inspect the details of a particular konduit server given an id. To find a list of running servers and their details, use the 'list' command.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Prints the inference configuration of server with an id of 'inf_server':\n" +
        "$ konduit inspect inf_server\n" +
        "--------------")
public class InspectCommand extends DefaultCommand {

    private String id;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void run() {
        if(LauncherUtils.isProcessExists(id)) {
            try {
                out.println(FileUtils.readFileToString(
                                new File(DirectoryFetcher.getServersDataDir(),
                                        LauncherUtils.getPidFromServerId(id) + ".data"),
                                StandardCharsets.UTF_8));
            } catch (Exception exception) {
                exception.printStackTrace(out);
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }

}
