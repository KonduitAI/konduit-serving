package ai.konduit.serving.configprovider;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;
import io.micrometer.core.instrument.MeterRegistry;
import  io.vertx.micrometer.backends.BackendRegistries;
import  io.vertx.core.logging.Logger;
import  java.io.File;
import  io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.config.ConfigRetriever;

public class KonduitServingNodeConfigurer {

    private String configHost;
    private int configPort;
    private String configStoreType;
    private String configPath = "/srv/";
    private boolean workerNode = true;
    private boolean ha = false;
    private int numInstances = 1;
    private int workerPoolSize = 20;
    private String verticleClassName = ai.konduit.serving.verticles.inference.InferenceVerticle.class.getName();
    private String vertxWorkingDirectory = System.getProperty("user.home");

    private MeterRegistry registry = io.vertx.micrometer.backends.BackendRegistries.getDefaultNow();
    private String pidFile = new java.io.File(System.getProperty("user.dir"),"pipelines.pid").getAbsolutePath();
    private long eventLoopTimeout = 120000;

    private long eventLoopExecutionTimeout = 120000;


    private static io.vertx.core.logging.Logger log = io.vertx.core.logging.LoggerFactory.getLogger(KonduitServingMain.class.getName());

    static {
        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
        io.vertx.core.logging.LoggerFactory.getLogger (io.vertx.core.logging.LoggerFactory.class); // Required for Logback to work in Vertx
    }



    private io.vertx.core.Vertx vertx;
    private io.vertx.core.Verticle verticle;

    public void runMain(String...args) {
        log.debug("Parsing args " + java.util.Arrays.toString(args));
        com.beust.jcommander.JCommander jCommander = new com.beust.jcommander.JCommander(this);
        jCommander.parse(args);
        io.vertx.core.json.JsonObject config = new io.vertx.core.json.JsonObject();

        java.io.File workingDir = new java.io.File(vertxWorkingDirectory);
        if (!workingDir.canRead() || !workingDir.canWrite()) {
            throw new IllegalStateException("Illegal Directory " + vertxWorkingDirectory + " unable to " +
                    "write or read. Please specify a proper vertx working directory");
        }


        try {

            long pid = getPid();
            java.io.File write = new java.io.File(pidFile);
            if(!write.getParentFile().exists()) {
                log.info("Creating parent directory for pid file");
                if(!write.getParentFile().mkdirs()) {
                    log.warn("Unable to create pid file directory.");
                }
            }

            log.info("Writing pid file to " + pidFile + " with pid " + pid);
            org.apache.commons.io.FileUtils.writeStringToFile(write,String.valueOf(pid), java.nio.charset.Charset.defaultCharset());
            write.deleteOnExit();
        }catch(Exception e) {
            log.warn("Unable to write pid file.",e);
        }

        setProperty("vertx.cwd", vertxWorkingDirectory);
        setProperty("vertx.cacheDirBase", vertxWorkingDirectory);
        setProperty("vertx.disableFileCPResolving", "true");
        //logging using slf4j: defaults to jul
        setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

        io.micrometer.prometheus.PrometheusMeterRegistry prometheusBackendRegistry = new io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);
        registry = prometheusBackendRegistry;


       MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(registry)
                .setPrometheusOptions(new io.vertx.micrometer.VertxPrometheusOptions()
                        .setEnabled(true));
       BackendRegistries.setupBackend(micrometerMetricsOptions);

        log.info("Setup micro meter options.");

        vertx = io.vertx.core.Vertx.vertx(new io.vertx.core.VertxOptions()
                .setMaxEventLoopExecuteTime(eventLoopExecutionTimeout)
                .setBlockedThreadCheckInterval(eventLoopTimeout)
                .setWorkerPoolSize(workerPoolSize)
                .setMetricsOptions(micrometerMetricsOptions));
        vertx.exceptionHandler(handler -> {
            log.error("Error occurred",handler.getCause());
            System.exit(1);
        });


        if (verticleClassName == null) {
            log.debug("Attempting to resolve verticle name");
            verticleClassName = ai.konduit.serving.verticles.inference.InferenceVerticle.class.getName();
        }

        String[] split = verticleClassName.split("\\.");
        vertx.registerVerticleFactory(new io.vertx.core.spi.VerticleFactory() {
            @Override
            public String prefix() {
                return split[split.length - 1];
            }

            @Override
            public io.vertx.core.Verticle createVerticle(String s, ClassLoader classLoader) throws Exception {
                verticle = (io.vertx.core.Verticle) classLoader.loadClass(verticleClassName).newInstance();
                log.debug("Setup verticle " + verticle);
                return verticle;
            }
        });


        if (configHost != null)
            config.put("host", configHost);

        if (configPath != null) {
            java.io.File tmpFile = new java.io.File(configPath);
            if(!tmpFile.exists()) {
                throw new IllegalStateException("Path " + tmpFile.getAbsolutePath() + " does not exist!");
            }
            else if(configPath.endsWith(".yml")) {
                java.io.File configInputYaml = new java.io.File(configPath);
                java.io.File tmpConfigJson = new java.io.File(configInputYaml.getParent(), java.util.UUID.randomUUID() + "-config.json");
                log.info("Rewriting yml " + configPath + " to json " + tmpConfigJson + " . THis file will disappear after server is stopped.");
                tmpConfigJson.deleteOnExit();

                try {
                    ai.konduit.serving.InferenceConfiguration inferenceConfiguration = ai.konduit.serving.InferenceConfiguration.fromYaml(
                            org.apache.commons.io.FileUtils.readFileToString(tmpFile,
                                    java.nio.charset.Charset.defaultCharset()));
                    org.apache.commons.io.FileUtils.writeStringToFile(tmpConfigJson,
                            inferenceConfiguration.toJson()
                            , java.nio.charset.Charset.defaultCharset());
                    configPath = tmpConfigJson.getAbsolutePath();
                    log.info("Rewrote input config yaml to path " + tmpConfigJson.getAbsolutePath());

                } catch (java.io.IOException e) {
                    log.error("Unable to rewrite configuration as json ",e);
                }
            }

            config.put("path", configPath);
        }

        if (configStoreType != null && configStoreType.equals("file")) {
            log.debug("Using file storage type.");
            if (!new java.io.File(configPath).exists()) {
                log.warn("No file found for config path. Exiting");
                System.exit(1);
            }
        }

        if(configStoreType == null) {
            configStoreType = "file";
        }

        io.vertx.config.ConfigStoreOptions httpStore = new io.vertx.config.ConfigStoreOptions()
                .setType(configStoreType)
                .setOptional(false)
                .setConfig(config);

        io.vertx.config.ConfigRetrieverOptions options = new io.vertx.config.ConfigRetrieverOptions()
                .addStore(httpStore);

        io.vertx.config.ConfigRetriever retriever = io.vertx.config.ConfigRetriever.create(vertx, options);
        java.util.concurrent.atomic.AtomicBoolean deployed = new java.util.concurrent.atomic.AtomicBoolean(false);

        /**
         * How to handle config for signle node vs cluster?
         */
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                log.error("Either unable to create configuration, or failed to download configuration",ar.cause());
                System.exit(1);
                // Failed to retrieve the configuration
            } else {
                io.vertx.core.json.JsonObject config2 = ar.result();
                if (!deployed.get()) {
                    io.vertx.core.DeploymentOptions deploymentOptions = new io.vertx.core.DeploymentOptions()
                            .setConfig(config2).setWorker(workerNode)
                            .setHa(ha).setInstances(numInstances)
                            .setExtraClasspath(java.util.Arrays.asList(System.getProperty("java.class.path").split(":")))
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
            return org.bytedeco.systems.global.windows.GetCurrentProcessId();
        }
        else if(org.apache.commons.lang3.SystemUtils.IS_OS_MAC) {
            return org.bytedeco.systems.global.macosx.getpid();

        }
        else {
            return org.bytedeco.systems.global.linux.getpid();
        }
    }

    public static void main(String...args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitServingMain().runMain(args);
            log.debug("Exiting model server.");
        }catch(Exception e) {
            log.error("Unable to start model server.",e);
            throw e;
        }
    }


}
