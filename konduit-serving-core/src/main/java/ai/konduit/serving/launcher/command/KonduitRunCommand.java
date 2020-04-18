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
import io.vertx.core.impl.launcher.CommandLineUtils;
import io.vertx.core.impl.launcher.commands.RunCommand;
import org.nd4j.shade.guava.base.Strings;

import java.io.File;
import java.util.Collections;

import static ai.konduit.serving.launcher.KonduitServingLauncher.*;

@Name(value = "run", priority = 1)
@Summary("Runs a konduit server in the foreground.")
@Description("Runs a konduit server in the foreground.")
public class KonduitRunCommand extends RunCommand {

    public static final String DEFAULT_SERVICE = "inference";

    @Override
    @Option(longName = "service", shortName = "s", argName = "type")
    @DefaultValue(DEFAULT_SERVICE)
    @Description("Service type that needs to be deployed. Defaults to \"inference\"")
    public void setMainVerticle(String konduitServiceType) {
        if(getServicesMap().containsKey(konduitServiceType)) {
            super.setMainVerticle(KONDUIT_PREFIX + ":" + konduitServiceType);
        } else {
            throw new CLIException(String.format("Invalid service type %s. " +
                    "Allowed values are: %s", konduitServiceType,
                    Collections.singletonList(getServicesMap().keySet())));
        }
    }

    /**
     * The main verticle configuration, it can be a json file or a json string.
     *
     * @param configuration the configuration
     */
    @Override
    @Option(shortName = "c", longName = "config", argName = "config", required = true)
    @Description("Specifies a configuration that should be provided to the verticle. <config> should reference either a " +
            "text file containing a valid JSON object which represents the configuration OR be a JSON string.")
    public void setConfig(String configuration) {
        File file = new File(configuration);
        if(file.exists()) {
            configuration = file.getAbsolutePath();
        }

        log.info("Processing configuration: {}", configuration);
        super.setConfig(configuration);
    }

    @Override
    public void run() {
        String serverId = getServerId();
        log.info("Starting konduit server with an id of '{}'", serverId);

        if(!Strings.isNullOrEmpty(serverId)) {
            LogUtils.setAppendersForRunCommand(serverId);
        }

        super.run();
    }

    private String getServerId() {
        String[] commandSplits = CommandLineUtils.getCommand().split(" ");
        String lastSegment = commandSplits[commandSplits.length - 1];
        if(lastSegment.contains("serving.id")) {
            return lastSegment.replace("-Dserving.id=", "").trim();
        } else {
            return null;
        }
    }
}