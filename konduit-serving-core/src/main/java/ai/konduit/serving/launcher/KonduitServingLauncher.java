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

package ai.konduit.serving.launcher;

import ai.konduit.serving.launcher.command.*;
import ai.konduit.serving.settings.Fetcher;
import ai.konduit.serving.util.LogUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.*;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.shade.guava.collect.ImmutableMap;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.file.FileSystemOptions.DEFAULT_FILE_CACHING_DIR;
import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static io.vertx.core.file.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

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

    public static final String KONDUIT_PREFIX = "konduit";

    public static final Map<String, String> services = ImmutableMap.of(
            "inference", InferenceVerticle.class.getCanonicalName(),
            "memmap", InferenceVerticle.class.getCanonicalName()
    );

    static {
        LogUtils.setAppendersForCommandLine();

        LauncherUtils.setCommonLoggingAndVertxProperties();
    }

    @Override
    protected String getMainVerticle() {
        return InferenceVerticle.class.getCanonicalName();
    }

    @Override
    protected String getDefaultCommand() {
        return "--help";
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true));

        log.info("Setup micro meter options.");
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        options.setMetricsOptions(micrometerMetricsOptions);
        options.setMaxEventLoopExecuteTime(60);
        options.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        vertx.registerVerticleFactory(new VerticleFactory() {

            @Override
            public String prefix() {
                return KONDUIT_PREFIX;
            }

            @Override
            public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
                String serviceType = verticleName.replace(KONDUIT_PREFIX + ":", "");

                if(services.containsKey(serviceType)) {
                    return (Verticle) ClassLoader.getSystemClassLoader().loadClass(services.get(serviceType)).newInstance();
                } else {
                    log.error("Invalid service type {}. Possible values are: {}", serviceType, services.keySet());
                    System.exit(1);
                    return null;
                }
            }
        });
    }

    public static void main(String[] args) {
        KonduitServingLauncher konduitServingLauncher = new KonduitServingLauncher().setMainCommands();

        if(args.length > 0 && RunCommand.class.getAnnotation(Name.class).value().equals(args[0]))
            konduitServingLauncher.register(RunCommand.class, RunCommand::new);

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
