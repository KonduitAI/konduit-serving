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

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.launcher.command.*;
import ai.konduit.serving.util.LogUtils;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.*;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.shade.guava.collect.ImmutableMap;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.file.FileSystemOptions.DEFAULT_FILE_CACHING_DIR;
import static io.vertx.core.file.impl.FileResolver.CACHE_DIR_BASE_PROP_NAME;
import static io.vertx.core.file.impl.FileResolver.DISABLE_CP_RESOLVING_PROP_NAME;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class KonduitServingLauncher extends Launcher {

    public static final String KONDUIT_PREFIX = "konduit";

    public static final Map<String, String> services = ImmutableMap.of(
            "inference", InferenceVerticle.class.getCanonicalName(),
            "memmap", InferenceVerticle.class.getCanonicalName()
    );

    InferenceConfiguration inferenceConfiguration;

    static {
        LogUtils.setAppendersForCommandLine();

        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx

        setProperty("vertx.cwd", new File(".").getAbsolutePath());
        setProperty(CACHE_DIR_BASE_PROP_NAME, DEFAULT_FILE_CACHING_DIR);
        setProperty(DISABLE_CP_RESOLVING_PROP_NAME, Boolean.TRUE.toString());
    }

    @Override
    protected String getMainVerticle() {
        return InferenceVerticle.class.getCanonicalName();
    }

    @Override
    protected String getDefaultCommand() {
        return ServeCommand.class.getAnnotation(Name.class).value();
    }

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
                    log.error(String.format("Invalid service type %s. Possible values are: ", Arrays.asList(services.keySet())));
                    System.exit(1);
                    return null;
                }
            }
        });
    }

    @Override
    public void afterConfigParsed(JsonObject config) {
        this.inferenceConfiguration = InferenceConfiguration.fromJson(config.encode());
    }

    public static void main(String[] args) {
        new KonduitServingLauncher()
                .unregister("bare")
                .unregister("start")
                .unregister("test")
                .unregister("version")
                .register(JoinCommand.class, JoinCommand::new)
                .register(ServeCommand.class, ServeCommand::new)
                .register(RunCommand.class, RunCommand::new)
                .register(ListCommand.class, ListCommand::new)
                .register(StopCommand.class, StopCommand::new)
                .register(PredictCommand.class, PredictCommand::new)
                .register(VersionCommand.class, VersionCommand::new)
                .register(ConfigCommand.class, ConfigCommand::new)
                .register(InspectCommand.class, InspectCommand::new)
                .register(LogsCommand.class, LogsCommand::new)
                .dispatch(args);
    }

    @Override
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
        log.error("\nFailed to start konduit server.\n");

        super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
    }
}
