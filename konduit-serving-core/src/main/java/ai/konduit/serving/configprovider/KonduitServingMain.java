/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
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
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.commons.io.FileUtils;
import org.bytedeco.systems.global.linux;
import org.bytedeco.systems.global.macosx;
import org.bytedeco.systems.global.windows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

/**
 * Main class for running pipelines.
 * This command line is an interface on top of vertx's
 * {@link DeploymentOptions} and contains a mix of parameters
 * for configuration where to download configuration
 * files from using {@link ConfigRetriever}
 * and the deployment options for the verticle
 * using vertx's {@link DeploymentOptions}
 * The {@link #configStoreType} is passed to
 * {@link ConfigStoreOptions}
 *
 * @author Adam Gibson
 */
public class KonduitServingMain {

    @Parameter(names = {"--configHost"},help = true, description = "The host for downloading the configuration from")
    private String configHost;
    @Parameter(names = {"--configPort"},help = true, description = "The port for downloading the configuration from")
    private int configPort;

    @Parameter(names = {"--configStoreType"},help = true, description = "The configuration store type (usually http " +
            "or file) where the configuration is stored")
    private String configStoreType;
    @Parameter(names = {"--configPath"},help = true, description = "The path to the configuration. With http, this " +
            "will be the path after host:port. With files, this will be an absolute path.")
    private String configPath = new File(System.getProperty("user.dir"), "config.json").getAbsolutePath();

    @Parameter(names = {"--workerNode"},help = true, description = "Whether this is a worker node or not")
    private boolean workerNode = true;
    @Parameter(names = {"--ha"}, help = true, description = "Whether this node is deployed as Highly available or not.")
    private boolean ha = false;
    @Parameter(names = {"--numInstances"}, help = true, description = "The number of instances to deploy of this verticle.")
    private int numInstances = 1;
    @Parameter(names = {"--workerPoolSize"}, help = true, description = "The number of workers for use with this verticle.")
    private int workerPoolSize = 20;
    @Parameter(names = {"--verticleClassName"}, help = true, description = "The fully qualified class name to the verticle to be used.")
    private String verticleClassName = InferenceVerticle.class.getName();
    @Parameter(names = "--vertxWorkingDirectory", help = true, description = "The absolute path to use for vertx. This defaults to the user's home directory.")
    private String vertxWorkingDirectory = System.getProperty("user.home");

    @Parameter(names = "--pidFile", help = true, description = "The absolute path to the PID file. This defaults to the current directory: pipelines.pid.")
    private String pidFile = new File(System.getProperty("user.dir"), "konduit-serving.pid").getAbsolutePath();

    @Parameter(names = {"--eventLoopTimeout"}, help = true, description = "The event loop timeout")
    private long eventLoopTimeout = 120000;

    @Parameter(names = {"--eventLoopExecutionTimeout"}, help = true, description = "The event loop timeout")
    private long eventLoopExecutionTimeout = 120000;

    private static Logger log = LoggerFactory.getLogger(KonduitServingMain.class.getName());

    static {
        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger (LoggerFactory.class); // Required for Logback to work in Vertx
    }

    public KonduitServingMain() {}

    private Vertx vertx;
    private Verticle verticle;

    public void runMain(String...args) {
        log.debug("Parsing args " + Arrays.toString(args));
        JCommander jCommander = new JCommander(this);
        jCommander.parse(args);
        JsonObject config = new JsonObject();

        File workingDir = new File(vertxWorkingDirectory);
        if (!workingDir.canRead() || !workingDir.canWrite()) {
            throw new IllegalStateException("Illegal Directory " + vertxWorkingDirectory + " unable to " +
                    "write or read. Please specify a proper vertx working directory");
        }

        try {
            long pid = getPid();
            File write = new File(pidFile);
            if(!write.getParentFile().exists()) {
                log.info("Creating parent directory for pid file");
                if(!write.getParentFile().mkdirs()) {
                    log.warn("Unable to create pid file directory.");
                }
            }

            log.info("Writing pid file to " + pidFile + " with pid " + pid);
            FileUtils.writeStringToFile(write, String.valueOf(pid), Charset.defaultCharset());
            write.deleteOnExit();
        } catch(Exception e) {
            log.warn("Unable to write pid file.", e);
        }

        setProperty("vertx.cwd", vertxWorkingDirectory);
        setProperty("vertx.cacheDirBase", vertxWorkingDirectory);
        setProperty("vertx.disableFileCPResolving", "true");
        //logging using slf4j: defaults to jul
        setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(registry)
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true));
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        log.info("Setup micro meter options.");

        vertx = Vertx.vertx(new VertxOptions()
                .setMaxEventLoopExecuteTime(eventLoopExecutionTimeout)
                .setBlockedThreadCheckInterval(eventLoopTimeout)
                .setWorkerPoolSize(workerPoolSize)
                .setMetricsOptions(micrometerMetricsOptions));

        vertx.exceptionHandler(handler -> {
            log.error("Error occurred", handler.getCause());
            System.exit(1);
        });

        if (verticleClassName == null) {
            log.debug("Attempting to resolve verticle name");
            verticleClassName = InferenceVerticle.class.getName();
        }

        String[] split = verticleClassName.split("\\.");
        vertx.registerVerticleFactory(new VerticleFactory() {
            @Override
            public String prefix() {
                return split[split.length - 1];
            }

            @Override
            public Verticle createVerticle(String s, ClassLoader classLoader) throws Exception {
                verticle = (Verticle) classLoader.loadClass(verticleClassName).newInstance();
                log.debug("Setup verticle " + verticle);
                return verticle;
            }
        });

        if (configHost != null)
            config.put("host", configHost);

        if (configPath != null) {
            File tmpFile = new File(configPath);
            if(!tmpFile.exists()) {
                throw new IllegalStateException("Path " + tmpFile.getAbsolutePath() + " does not exist!");
            }
            else if(configPath.endsWith(".yml")) {
                File configInputYaml = new File(configPath);
                File tmpConfigJson = new File(configInputYaml.getParent(), UUID.randomUUID() + "-config.json");
                log.info("Rewriting yml " + configPath + " to json " + tmpConfigJson + " . THis file will disappear after server is stopped.");
                tmpConfigJson.deleteOnExit();

                try {
                    InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromYaml(
                            FileUtils.readFileToString(tmpFile,
                                    Charset.defaultCharset()));
                    FileUtils.writeStringToFile(tmpConfigJson,
                            inferenceConfiguration.toJson(),
                            Charset.defaultCharset());
                    configPath = tmpConfigJson.getAbsolutePath();
                    log.info("Rewrote input config yaml to path " + tmpConfigJson.getAbsolutePath());

                } catch (IOException e) {
                    log.error("Unable to rewrite configuration as json ", e);
                }
            }
            config.put("path", configPath);
        }

        if (configStoreType != null && configStoreType.equals("file")) {
            log.debug("Using file storage type.");
            if (!new File(configPath).exists()) {
                log.warn("No file found for config path. Exiting");
                System.exit(1);
            }
        }

        if(configStoreType == null) {
            configStoreType = "file";
        }

        ConfigStoreOptions httpStore = new ConfigStoreOptions()
                .setType(configStoreType)
                .setOptional(false)
                .setConfig(config);

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(httpStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        AtomicBoolean deployed = new AtomicBoolean(false);

        retriever.getConfig(ar -> {
            if (ar.failed()) {
                log.error("Either unable to create configuration, or failed to download configuration", ar.cause());
                System.exit(1);
                // Failed to retrieve the configuration
            } else {
                JsonObject config2 = ar.result();
                if (!deployed.get()) {
                    DeploymentOptions deploymentOptions = new DeploymentOptions()
                            .setConfig(config2).setWorker(workerNode)
                            .setHa(ha).setInstances(numInstances)
                            .setExtraClasspath(Arrays.asList(System.getProperty("java.class.path").split(":")))
                            .setWorkerPoolSize(workerPoolSize);

                    log.debug("Attempting to deploy verticle " + verticleClassName);
                    vertx.deployVerticle(verticleClassName, deploymentOptions, handler -> {
                        if (handler.failed()) {
                            log.error("Failed to deploy verticle. Exiting. ", handler.cause());
                            vertx.close();
                        }
                        else {
                            log.info("Started verticle. ", handler.cause());

                        }
                    });
                    deployed.set(true);
                } else {
                    log.debug("Verticle already deployed.");
                }
            }
        });
    }

    private int getPid() throws UnsatisfiedLinkError {
        if(org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
            return windows.GetCurrentProcessId();
        }
        else if(org.apache.commons.lang3.SystemUtils.IS_OS_MAC) {
            return macosx.getpid();
        } else {
            return linux.getpid();
        }
    }

    public static void main(String...args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitServingMain().runMain(args);
            log.debug("Exiting model server.");
         }catch(Exception e) {
            log.error("Unable to start model server.", e);
            throw e;
        }
    }
}
