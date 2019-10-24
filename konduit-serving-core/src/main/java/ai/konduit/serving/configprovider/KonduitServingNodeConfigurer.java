package ai.konduit.serving.configprovider;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;
import io.micrometer.core.instrument.MeterRegistry;
import  io.vertx.micrometer.backends.BackendRegistries;
import  io.vertx.core.logging.Logger;
import  io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import  java.io.File;
import  io.vertx.micrometer.MicrometerMetricsOptions;
import  io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.VertxOptions;
import com.beust.jcommander.Parameter;
import  io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;

@lombok.NoArgsConstructor
@lombok.Getter
public class KonduitServingNodeConfigurer {

    @Parameter(names = {"--configHost"},help = true,description = "The host for downloading the configuration from")
    private String configHost;
    @Parameter(names = {"--configPort"},help = true,description = "The port for downloading the configuration from")
    private int configPort;


    @lombok.Builder.Default
    @Parameter(names = {"--eventBusHost"},help = true,description = "The event bus host for connecting to other vertx nodes.")
    private String eventBusHost = "0.0.0.0";
    @lombok.Builder.Default
    @Parameter(names = {"--eventBusPort"},help = true,description = "The event bus port for connecting to other vertx nodes.")
    private int eventBusPort = 0;
    @lombok.Builder.Default
    @Parameter(names = {"--eventBusConnectTimeout"},help = true,description = "The timeout for connecting to an event bus.")
    private int eventBusConnectTimeout = 20000;

    @Parameter(names = {"--configStoreType"},help = true,description = "The configuration store type (usually http " +
            "or file) where the configuration is stored")
    @lombok.Builder.Default
    private String configStoreType = "file";

    @lombok.Builder.Default
    @Parameter(names = {"--configPath"},help = true,description = "The path to the configuration. With http, this " +
            "will be the path after host:port. With files, this will be an absolute path.")
    private String configPath = "/srv/";
    @lombok.Builder.Default
    @Parameter(names = {"--workerNode"},help = true,description = "Whether this is a worker node or not")
    private boolean workerNode = true;
    @lombok.Builder.Default
    @Parameter(names = {"--ha"},help = true,description = "Whether this node is deployed as Highly available or not.")
    private boolean ha = false;
    @lombok.Builder.Default
    @Parameter(names = {"--numInstances"},help = true,description = "The number of instances to deploy of this verticle.")
    private int numInstances = 1;
    @lombok.Builder.Default
    @Parameter(names = {"--workerPoolSize"},help = true,description = "The number of workers for use with this verticle.")
    private int workerPoolSize = 20;
    @lombok.Builder.Default
    @Parameter(names = {"--verticleClassName"},help = true,description = "The fully qualified class name to the verticle to be used.")
    private String verticleClassName = ai.konduit.serving.verticles.inference.InferenceVerticle.class.getName();
    @lombok.Builder.Default
    @Parameter(names = "--vertxWorkingDirectory",help = true,description = "The absolute path to use for vertx. This defaults to the user's home directory.")
    private String vertxWorkingDirectory = System.getProperty("user.home");

    private MeterRegistry registry = io.vertx.micrometer.backends.BackendRegistries.getDefaultNow();
    @lombok.Builder.Default
    private String pidFile = new File(System.getProperty("user.dir"),"pipelines.pid").getAbsolutePath();
    @lombok.Builder.Default
    @Parameter(names = {"--eventLoopTimeout"},help = true,description = "The event loop timeout")
    private long eventLoopTimeout = 120000;
    @lombok.Builder.Default
    @Parameter(names = {"--eventLoopExecutionTimeout"},help = true,description = "The event loop timeout")
    private long eventLoopExecutionTimeout = 120000;

    private  ConfigStoreOptions httpStore;
    private  DeploymentOptions deploymentOptions;
    private  ConfigRetrieverOptions options;
    private  VertxOptions vertxOptions;

    private static Logger log = LoggerFactory.getLogger(KonduitServingMain.class.getName());

    static {
        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME,SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger (LoggerFactory.class); // Required for Logback to work in Vertx
    }





    @lombok.Builder
    public KonduitServingNodeConfigurer(long eventLoopExecutionTimeout,
                                        long eventLoopTimeout,
                                        int numInstances,
                                        int workerPoolSize,
                                        String vertxWorkingDirectory,
                                        String pidFile,
                                        String configPath,
                                        String eventBusHost,
                                        String verticleClassName,
                                        String configHost,
                                        int configPort,
                                        int eventBusPort,
                                        int eventBusConnectTimeout) {
        this.verticleClassName = verticleClassName;
        this.configHost = configHost;
        this.configPort = configPort;
        this.eventLoopExecutionTimeout = eventLoopExecutionTimeout;
        this.eventLoopTimeout = eventLoopTimeout;
        this.pidFile = pidFile;
        this.eventBusHost = eventBusHost;
        this.eventBusPort = eventBusPort;
        this.eventBusConnectTimeout = eventBusConnectTimeout;
        this.configPath= configPath;
        this.numInstances = numInstances;
        this.workerPoolSize = workerPoolSize;
        this.vertxWorkingDirectory = vertxWorkingDirectory;

    }


    public void setupVertxOptions() {
        File workingDir = new File(vertxWorkingDirectory);
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

        PrometheusMeterRegistry prometheusBackendRegistry = new PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);
        registry = prometheusBackendRegistry;


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


        vertxOptions.getEventBusOptions().setClustered(true);
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


        options = new ConfigRetrieverOptions()
                .addStore(httpStore);

    }


    public void configureWithJson(io.vertx.core.json.JsonObject config) {
        httpStore = new ConfigStoreOptions()
                .setType(configStoreType)
                .setOptional(false)
                .setConfig(config);



        deploymentOptions = new DeploymentOptions()
                .setWorker(workerNode)
                .setHa(ha).setInstances(numInstances)
                .setConfig(config)
                .setExtraClasspath(java.util.Arrays.asList(System.getProperty("java.class.path").split(":")))
                .setWorkerPoolSize(workerPoolSize);

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
