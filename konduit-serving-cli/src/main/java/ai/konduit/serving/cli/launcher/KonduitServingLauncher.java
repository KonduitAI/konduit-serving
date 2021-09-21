/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
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

import ai.konduit.serving.build.cli.BuildCLI;
import ai.konduit.serving.cli.launcher.command.*;
import ai.konduit.serving.cli.launcher.command.build.extension.ProfileCommand;
import ai.konduit.serving.cli.launcher.command.build.extension.PythonPathsCommand;
import ai.konduit.serving.cli.launcher.command.build.extension.ServeBuildCommand;
import ai.konduit.serving.pipeline.settings.constants.Constants;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.annotations.Name;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Initializes the {@link VertxOptions} for deployment and use in a
 * {@link Vertx} instance.
 * The following other initialization also happens:
 * {@code Vertx Working Directory} gets set (vertx.cwd) and {vertx.caseDirBase)
 * (vertx.disableFileCPResolving) gets set to true
 * (vertx.logger-delegate-factory-class-name) gets set to io.vertx.core.logging.SLF4JLogDelegateFactory
 * The {@link MeterRegistry} field and associated prometheus configuration gets setup
 * The {@link VertxOptions} event but options also get set
 */
@Slf4j
public class KonduitServingLauncher extends Launcher {

    @Override
    protected String getDefaultCommand() {
        return "--help";
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        LauncherUtils.setCommonVertxProperties();

        options.setMaxEventLoopExecuteTime(600);
        options.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        new KonduitServingLauncher().exec(args);
    }

    public void exec(String[] args) {
        ObjectMappers.json().setDateFormat(new SimpleDateFormat(Constants.DATE_FORMAT));

        this.setMainCommands();

        if(args.length > 0 && KonduitRunCommand.class.getAnnotation(Name.class).value().equals(args[0]))
            this.register(KonduitRunCommand.class, KonduitRunCommand::new);

        this.dispatch(args);
    }

    public void setMainCommands() {
        this.unregister("bare")
            .unregister("start")
            .unregister("run")
            .unregister("test")
            .unregister("version")
            //.register(JoinCommand.class, JoinCommand::new) // TODO: Uncomment this after implementation and testing
            .register(ServeBuildCommand.class, ServeBuildCommand::new)
            .register(ListCommand.class, ListCommand::new)
            .register(StopCommand.class, StopCommand::new)
            .register(MetricsCommand.class, MetricsCommand::new)
            .register(VersionCommand.class, VersionCommand::new)
            .register(ConfigCommand.class, ConfigCommand::new)
            .register(InspectCommand.class, InspectCommand::new)
            .register(LogsCommand.class, LogsCommand::new)
            .register(ProfileCommand.class, ProfileCommand::new)
            .register(BuildCLI.class, BuildCLI::new)
            .register(PythonPathsCommand.class, PythonPathsCommand::new);
    }

    public String commandLinePrefix() {
        return getCommandLinePrefix();
    }
}
