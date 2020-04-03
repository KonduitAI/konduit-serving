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

import io.vertx.core.cli.annotations.*;
import io.vertx.core.impl.launcher.commands.StartCommand;

@Name("serve")
@Summary("Start a konduit server application in background")
@Description("Start a konduit server application as a background service. " +
        "The application is identified with an id that can be set using the `--serving-id` or `-id` option. " +
        "If not set, a random UUID is generated. " +
        "The application can be stopped with the `stop` command." +
        "This command takes the `run` command parameters. To see the " +
        "run command parameters, execute `run --help`")
public class ServeCommand extends StartCommand {

    /**
     * Sets the "application id" that would be to stop the application and be listed in the {@link ListCommand} command.
     *
     * @param id the application ID.
     */
    @Option(longName = "serving-id", shortName = "id", required = true)
    @Description("Id of the serving process. This will be visible in the \'list\' command. This id can be used to call \'predict\' and \'stop\' commands on the running servers.")
    public void setApplicationId(String id) {
        super.setApplicationId(id);
    }

}
