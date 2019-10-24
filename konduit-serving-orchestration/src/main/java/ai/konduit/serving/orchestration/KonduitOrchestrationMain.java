package ai.konduit.serving.orchestration;

import io.vertx.core.Vertx;

public class KonduitOrchestrationMain {

    private static io.vertx.core.logging.Logger log = io.vertx.core.logging.LoggerFactory.getLogger(ai.konduit.serving.configprovider.KonduitServingMain.class.getName());



    public static void main(String...args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitOrchestrationMain().runMain(args);
            log.debug("Exiting model server.");
        }catch(Exception e) {
            log.error("Unable to start model server.",e);
            throw e;
        }
    }

    public void runMain(String... args) {
        log.debug("Parsing args " + java.util.Arrays.toString(args));
        ai.konduit.serving.configprovider.KonduitServingNodeConfigurer konduitServingNodeConfigurer = new ai.konduit.serving.configprovider.KonduitServingNodeConfigurer();
        com.beust.jcommander.JCommander jCommander = new com.beust.jcommander.JCommander(konduitServingNodeConfigurer);
        jCommander.parse(args);
        konduitServingNodeConfigurer.setupVertxOptions();
        Vertx.clusteredVertx(konduitServingNodeConfigurer.getVertxOptions(),vertxAsyncResult -> {
            Vertx vertx = vertxAsyncResult.result();
            io.vertx.config.ConfigRetriever configRetriever = io.vertx.config.ConfigRetriever.create(vertx,konduitServingNodeConfigurer.getOptions());
            configRetriever.getConfig(result -> {
                if(result.failed()) {
                    log.error("Unable to retrieve configuration " + result.cause());
                }
                else {
                    io.vertx.core.json.JsonObject result1 = result.result();
                    konduitServingNodeConfigurer.configureWithJson(result1);
                    vertx.deployVerticle(konduitServingNodeConfigurer.getVerticleClassName(),konduitServingNodeConfigurer.getDeploymentOptions(),handler -> {
                        if(handler.failed()) {
                            log.error("Unable to deploy verticle {}",konduitServingNodeConfigurer.getVerticleClassName(),handler.cause());
                        }
                        else {
                            log.info("Deployed verticle {}",konduitServingNodeConfigurer.getVerticleClassName());
                        }
                    });
                }
            });
        });
    }

}
