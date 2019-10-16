package ai.konduit.serving.orchestration;

import  io.vertx.core.spi.cluster.ClusterManager;
import  io.vertx.core.VertxOptions;
import io.vertx.core.Vertx;
import  io.vertx.core.DeploymentOptions;

import ai.konduit.serving.verticles.inference.InferenceVerticle;

public class PipelineOrchestrationMain {

    private ClusterManager clusterManager;
    private String eventBusHost = "0.0.0.0";
    private int eventBusPort = 0;
    private int eventBusConnectTimeout = 20000;

    public static void main(String...args) {

    }

    public void runMain(String... args) {
        VertxOptions options = new VertxOptions()
                .setClusterManager(clusterManager);
        options.getEventBusOptions().setClustered(true);
        options.getEventBusOptions().setPort(eventBusPort);
        options.getEventBusOptions().setHost(eventBusHost);
        options.getEventBusOptions().setLogActivity(true);
        options.getEventBusOptions().setConnectTimeout(eventBusConnectTimeout);


        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();
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
    }


}
