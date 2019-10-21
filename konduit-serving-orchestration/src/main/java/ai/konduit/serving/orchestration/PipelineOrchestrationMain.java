package ai.konduit.serving.orchestration;

import  io.vertx.core.spi.cluster.ClusterManager;
import  io.vertx.core.VertxOptions;
import io.vertx.core.Vertx;
import  io.vertx.core.DeploymentOptions;

import ai.konduit.serving.verticles.inference.InferenceVerticle;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

public class PipelineOrchestrationMain {

    private ClusterManager clusterManager;
    private String eventBusHost = "0.0.0.0";
    private int eventBusPort = 0;
    private int eventBusConnectTimeout = 20000;


    @com.beust.jcommander.Parameter(names = {"--configHost"},help = true,description = "The host for downloading the configuration from")
    private String configHost;
    @com.beust.jcommander.Parameter(names = {"--configPort"},help = true,description = "The port for downloading the configuration from")
    private int configPort;


    @com.beust.jcommander.Parameter(names = {"--configStoreType"},help = true,description = "The configuration store type (usually http " +
            "or file) where the configuration is stored")
    private String configStoreType;
    @com.beust.jcommander.Parameter(names = {"--configPath"},help = true,description = "The path to the configuration. With http, this " +
            "will be the path after host:port. With files, this will be an absolute path.")
    private String configPath = "/srv/";

    @com.beust.jcommander.Parameter(names = {"--workerNode"},help = true,description = "Whether this is a worker node or not")
    private boolean workerNode = true;
    @com.beust.jcommander.Parameter(names = {"--ha"},help = true,description = "Whether this node is deployed as Highly available or not.")
    private boolean ha = false;
    @com.beust.jcommander.Parameter(names = {"--numInstances"},help = true,description = "The number of instances to deploy of this verticle.")
    private int numInstances = 1;
    @com.beust.jcommander.Parameter(names = {"--workerPoolSize"},help = true,description = "The number of workers for use with this verticle.")
    private int workerPoolSize = 20;
    @com.beust.jcommander.Parameter(names = {"--verticleClassName"},help = true,description = "The fully qualified class name to the verticle to be used.")
    private String verticleClassName = InferenceVerticle.class.getName();
    @com.beust.jcommander.Parameter(names = "--vertxWorkingDirectory",help = true,description = "The absolute path to use for vertx. This defaults to the user's home directory.")
    private String vertxWorkingDirectory = System.getProperty("user.home");

    private io.micrometer.core.instrument.MeterRegistry registry = io.vertx.micrometer.backends.BackendRegistries.getDefaultNow();
    @com.beust.jcommander.Parameter(names = "--pidFile",help = true,description = "The absolute path to the PID file. This defaults to the current directory: pipelines.pid.")
    private String pidFile = new java.io.File(System.getProperty("user.dir"),"pipelines.pid").getAbsolutePath();

    @com.beust.jcommander.Parameter(names = {"--eventLoopTimeout"},help = true,description = "The event loop timeout")
    private long eventLoopTimeout = 120000;

    @com.beust.jcommander.Parameter(names = {"--eventLoopExecutionTimeout"},help = true,description = "The event loop timeout")
    private long eventLoopExecutionTimeout = 120000;

    private static io.vertx.core.logging.Logger log = io.vertx.core.logging.LoggerFactory.getLogger(ai.konduit.serving.configprovider.KonduitServingMain.class.getName());

    static {
        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
        io.vertx.core.logging.LoggerFactory.getLogger (io.vertx.core.logging.LoggerFactory.class); // Required for Logback to work in Vertx

    }


    public static void main(String...args) {

    }

    public void runMain(String... args) {
        log.debug("Parsing args " + java.util.Arrays.toString(args));
        com.beust.jcommander.JCommander jCommander = new com.beust.jcommander.JCommander(this);
        jCommander.parse(args);

        io.vertx.micrometer.MicrometerMetricsOptions micrometerMetricsOptions = new io.vertx.micrometer.MicrometerMetricsOptions()
                .setMicrometerRegistry(registry)
                .setPrometheusOptions(new io.vertx.micrometer.VertxPrometheusOptions()
                        .setEnabled(true));
        io.vertx.micrometer.backends.BackendRegistries.setupBackend(micrometerMetricsOptions);

        VertxOptions vertxOptions = new VertxOptions()
                .setClusterManager(clusterManager);
        vertxOptions.setMaxEventLoopExecuteTime(eventLoopExecutionTimeout)
                .setBlockedThreadCheckInterval(eventLoopTimeout)
                .setWorkerPoolSize(workerPoolSize)
                .setMetricsOptions(micrometerMetricsOptions));
        vertxOptions.getEventBusOptions().setClustered(true);
        vertxOptions.getEventBusOptions().setPort(eventBusPort);
        vertxOptions.getEventBusOptions().setHost(eventBusHost);
        vertxOptions.getEventBusOptions().setLogActivity(true);
        vertxOptions.getEventBusOptions().setConnectTimeout(eventBusConnectTimeout);
        io.vertx.core.json.JsonObject config = new io.vertx.core.json.JsonObject();


        Vertx.clusteredVertx(vertxOptions, res -> {
            if (res.succeeded()) {
                final  Vertx vertx = res.result();


                if (verticleClassName == null) {
                    log.debug("Attempting to resolve verticle name");
                    verticleClassName = InferenceVerticle.class.getName();
                }

                String[] split = verticleClassName.split("\\.");



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

                retriever.getConfig(ar -> {
                    if (ar.failed()) {
                        log.error("Either unable to create configuration, or failed to download configuration",ar.cause());
                        System.exit(1);
                        // Failed to retrieve the configuration
                    } else {
                        io.vertx.core.json.JsonObject config2 = ar.result();
                        if (!deployed.get()) {
                            DeploymentOptions deploymentOptions = new DeploymentOptions()
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


                log.info("Setup micro meter options.");

                vertx.exceptionHandler(handler -> {
                    log.error("Error occurred",handler.getCause());
                    System.exit(1);
                });


                /**
                 * Figure out how deploying inference verticle and loading resources will work.
                 *
                 * Also look at how to resolve paths for deployment.
                 *
                 * Need to figure out if we want to allow more than 1 configuration object here?
                 * Also, do we persist state only in memory?
                 *
                 * Could maybe replicate all configurations over event bus?
                 *
                 * Could embed the file server verticle maybe?
                 * All we would need to do if a built in file server exists is
                 * work on a way to update the paths in the configuration next.
                 */
                DeploymentOptions deploymentOptions = new DeploymentOptions()
                        .setConfig(null)
                        .setInstances(1);
                vertx.deployVerticle(InferenceVerticle.class.getName(), deploymentOptions);
            }
        });



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

}
