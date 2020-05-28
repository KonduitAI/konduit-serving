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

package ai.konduit.serving.cli.launcher;

import ai.konduit.serving.cli.launcher.command.*;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.annotations.Name;
import lombok.extern.slf4j.Slf4j;

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

    static {
        LauncherUtils.setCommonVertxProperties();
    }

    @Override
    protected String getDefaultCommand() {
        return "--help";
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        options.setMaxEventLoopExecuteTime(60);
        options.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        DeployKonduitServing.registerInferenceVerticleFactory(vertx);
    }

    public static void main(String[] args) {
        KonduitServingLauncher konduitServingLauncher = new KonduitServingLauncher().setMainCommands();

        if(args.length > 0 && KonduitRunCommand.class.getAnnotation(Name.class).value().equals(args[0]))
            konduitServingLauncher.register(KonduitRunCommand.class, KonduitRunCommand::new);

        konduitServingLauncher.dispatch(args);
    }

    public KonduitServingLauncher setMainCommands() {
        return (KonduitServingLauncher) this
                .unregister("bare")
                .unregister("start")
                .unregister("run")
                .unregister("test")
                .unregister("version")
                //.register(JoinCommand.class, JoinCommand::new) // TODO: Uncomment this after implementation and testing
                .register(ServeCommand.class, ServeCommand::new)
                .register(ListCommand.class, ListCommand::new)
                .register(StopCommand.class, StopCommand::new)
                .register(PredictCommand.class, PredictCommand::new)
                .register(VersionCommand.class, VersionCommand::new)
                .register(ConfigCommand.class, ConfigCommand::new)
                .register(InspectCommand.class, InspectCommand::new)
                .register(LogsCommand.class, LogsCommand::new);
    }

    @Override
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
        log.error("\nFailed to start konduit server.\n");

        super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
    }

    public String commandLinePrefix() {
        return getCommandLinePrefix();
    }
}
