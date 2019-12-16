/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */


package ai.konduit.serving.configprovider;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.beust.jcommander.Parameter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

/**
 * Core node configurer based on both command line and builder arguments.
 * This contains the core initialization logic for initializing any peers
 * in a konduit serving cluster. This includes all of the interaction with vertx
 * such as metrics initialization, logging,..
 *
 * @author Adam Gibson
 * {@link #configureWithJson(JsonObject)}
 * {@link #setupVertxOptions()}
 * <p>
 * Generally, you call setup vertx options first.
 * This provides the necessary configuration to create a vertx instance.
 * After the vertx instance is created, then you can call configureWithJson
 * to give you a completed object.
 */

@lombok.NoArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
@lombok.Builder
public class KonduitServingNodeConfigurer {

    private static Logger log = LoggerFactory.getLogger(KonduitServingMain.class.getName());

    static {
        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx
    }

    @Parameter(names = {"--configHost"}, help = true, description = "The host for downloading the configuration from")
    private String configHost;
    @Parameter(names = {"--configPort"}, help = true, description = "The port for downloading the configuration from")
    private int configPort;
    @lombok.Builder.Default
    @Parameter(names = {"--eventBusHost"}, help = true, description = "The event bus host for connecting to other vertx nodes.")
    private String eventBusHost = "0.0.0.0";
    @lombok.Builder.Default
    @Parameter(names = {"--eventBusPort"}, help = true, description = "The event bus port for connecting to other vertx nodes.")
    private int eventBusPort = 0;
    @lombok.Builder.Default
    @Parameter(names = {"--eventBusConnectTimeout"}, help = true, description = "The timeout for connecting to an event bus.")
    private int eventBusConnectTimeout = 20000;
    @Parameter(names = {"--configStoreType"}, help = true, description = "The configuration store type (usually http " +
            "or file) where the configuration is stored")
    @lombok.Builder.Default
    private String configStoreType = "file";
    @lombok.Builder.Default
    @Parameter(names = {"--configPath"}, help = true, description = "The path to the configuration. With http, this " +
            "will be the path after host:port. With files, this will be an absolute path.")
    private String configPath = "/srv/";
    @lombok.Builder.Default
    @Parameter(names = {"--workerNode"}, help = true, description = "Whether this is a worker node or not")
    private boolean workerNode = true;
    @lombok.Builder.Default
    @Parameter(names = {"--ha"}, help = true, description = "Whether this node is deployed as Highly available or not.")
    private boolean ha = false;
    @lombok.Builder.Default
    @Parameter(names = {"--numInstances"}, help = true, description = "The number of instances to deploy of this verticle.")
    private int numInstances = 1;
    @lombok.Builder.Default
    @Parameter(names = {"--workerPoolSize"}, help = true, description = "The number of workers for use with this verticle.")
    private int workerPoolSize = 20;
    @lombok.Builder.Default
    @Parameter(names = {"--verticleClassName"}, help = true, description = "The fully qualified class name to the verticle to be used.")
    private String verticleClassName = InferenceVerticle.class.getName();
    @lombok.Builder.Default
    @Parameter(names = "--vertxWorkingDirectory", help = true, description = "The absolute path to use for vertx. This defaults to the user's home directory.")
    private String vertxWorkingDirectory = System.getProperty("user.home");
    private MeterRegistry registry = BackendRegistries.getDefaultNow();
    @lombok.Builder.Default
    @Parameter(names = "--pidFile", help = true, description = "The absolute path to use for creating the pid file. This defaults to the <current_dir>/konduit-serving.pid")
    private String pidFile = new File(System.getProperty("user.dir"), "konduit-serving.pid").getAbsolutePath();
    @lombok.Builder.Default
    @Parameter(names = {"--eventLoopTimeout"}, help = true, description = "The event loop timeout")
    private long eventLoopTimeout = 120000;
    @lombok.Builder.Default
    @Parameter(names = {"--eventLoopExecutionTimeout"}, help = true, description = "The event loop timeout")
    private long eventLoopExecutionTimeout = 120000;
    @lombok.Builder.Default
    @Parameter(names = {"--isClustered"}, help = true, description = "Whether an instance is clustered or not")
    private boolean isClustered = false;
    private ConfigStoreOptions httpStore;
    private DeploymentOptions deploymentOptions;
    private ConfigRetrieverOptions options;
    private VertxOptions vertxOptions;
    private InferenceConfiguration inferenceConfiguration;

    /**
     * Initializes the {@link VertxOptions} for deployment and use in a
     * {@link Vertx} instance.
     * The following other initialization also happens:
     * {@link #pidFile} gets written (temporarily)
     * {@link #vertxWorkingDirectory} gets set (vertx.cwd) and {vertx.caseDirBase)
     * (vertx.disableFileCPResolving) gets set to true
     * (vertx.logger-delegate-factory-class-name) gets set to io.vertx.core.logging.SLF4JLogDelegateFactory
     * The {@link #registry} field and associated prometheus configuration gets setup
     * The {@link #vertxOptions} event but options also get set
     * Lastly the {@link #options} gets set for the configuration
     */
    public void setupVertxOptions() {
        File workingDir = new File(vertxWorkingDirectory);
        if (!workingDir.canRead() || !workingDir.canWrite()) {
            throw new IllegalStateException("Illegal Directory " + vertxWorkingDirectory + " unable to " +
                    "write or read. Please specify a proper vertx working directory");
        }

        try {

            long pid = getPid();
            File write = new File(pidFile);
            if (!write.getParentFile().exists()) {
                log.info("Creating parent directory for pid file");
                if (!write.getParentFile().mkdirs()) {
                    log.warn("Unable to create pid file directory.");
                }
            }

            log.info("Writing pid file to " + pidFile + " with pid " + pid);
            FileUtils.writeStringToFile(write, String.valueOf(pid), Charset.defaultCharset());
            write.deleteOnExit();
        } catch (Exception e) {
            log.warn("Unable to write pid file.", e);
        }

        setProperty("vertx.cwd", vertxWorkingDirectory);
        setProperty("vertx.cacheDirBase", vertxWorkingDirectory);
        setProperty("vertx.disableFileCPResolving", "true");
        //logging using slf4j: defaults to jul
        setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(registry)
                .setPrometheusOptions(new io.vertx.micrometer.VertxPrometheusOptions()
                        .setEnabled(true));
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        log.info("Setup micro meter options.");

        BackendRegistries.setupBackend(micrometerMetricsOptions);

        vertxOptions = new VertxOptions()
                .setMaxEventLoopExecuteTime(eventLoopExecutionTimeout)
                .setBlockedThreadCheckInterval(eventLoopTimeout)
                .setWorkerPoolSize(workerPoolSize)
                .setMetricsOptions(micrometerMetricsOptions);

        vertxOptions.getEventBusOptions().setClustered(isClustered);
        vertxOptions.getEventBusOptions().setPort(eventBusPort);
        vertxOptions.getEventBusOptions().setHost(eventBusHost);
        vertxOptions.getEventBusOptions().setLogActivity(true);
        vertxOptions.getEventBusOptions().setConnectTimeout(eventBusConnectTimeout);

        if (verticleClassName == null) {
            log.debug("Attempting to resolve verticle name");
            verticleClassName = ai.konduit.serving.verticles.inference.InferenceVerticle.class.getName();
        }

        if (configStoreType != null && configStoreType.equals("file")) {
            log.debug("Using file storage type.");
            if (!new File(configPath).exists()) {
                log.warn("No file found for config path. Exiting");
                System.exit(1);
            }
        }

        httpStore = new ConfigStoreOptions()
                .setType(configStoreType)
                .setOptional(false)
                .setConfig(new JsonObject().put("path", configPath));


        options = new ConfigRetrieverOptions()
                .addStore(httpStore);

    }

    /**
     * Configure the deployment options
     * and associated {@link InferenceConfiguration}
     *
     * @param config the configuration to use for setup
     */
    public void configureWithJson(JsonObject config) {
        deploymentOptions = new DeploymentOptions()
                .setWorker(workerNode)
                .setHa(ha).setInstances(numInstances)
                .setConfig(config)
                .setExtraClasspath(java.util.Arrays.asList(System.getProperty("java.class.path").split(":")))
                .setWorkerPoolSize(workerPoolSize);

        if (configHost != null)
            config.put("host", configHost);

        if (configPath != null) {
            File tmpFile = new File(configPath);
            try {
                inferenceConfiguration = InferenceConfiguration.fromJson(
                        FileUtils.readFileToString(tmpFile,
                                Charset.defaultCharset()));
            } catch (IOException e) {
                log.error("Unable to read inference configuration with path " + configPath, e);
                return;
            }

            if (!tmpFile.exists()) {
                throw new IllegalStateException("Path " + tmpFile.getAbsolutePath() + " does not exist!");
            } else if (configPath.endsWith(".yml")) {
                File configInputYaml = new File(configPath);
                File tmpConfigJson = new File(configInputYaml.getParent(), java.util.UUID.randomUUID() + "-config.json");
                log.info("Rewriting yml " + configPath + " to json " + tmpConfigJson + " . This file will disappear after server is stopped.");
                tmpConfigJson.deleteOnExit();

                try {
                    inferenceConfiguration = InferenceConfiguration.fromYaml(
                            FileUtils.readFileToString(tmpFile,
                                    Charset.defaultCharset()));
                    FileUtils.writeStringToFile(tmpConfigJson,
                            inferenceConfiguration.toJson()
                            , Charset.defaultCharset());
                    configPath = tmpConfigJson.getAbsolutePath();
                    log.info("Rewrote input config yaml to path " + tmpConfigJson.getAbsolutePath());

                } catch (IOException e) {
                    log.error("Unable to rewrite configuration as json ", e);
                }
            }

            config.put("path", configPath);
        }

    }

    private int getPid() throws UnsatisfiedLinkError {
        if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
            return org.bytedeco.systems.global.windows.GetCurrentProcessId();
        } else if (org.apache.commons.lang3.SystemUtils.IS_OS_MAC) {
            return org.bytedeco.systems.global.macosx.getpid();
        } else {
            return org.bytedeco.systems.global.linux.getpid();
        }
    }
}
