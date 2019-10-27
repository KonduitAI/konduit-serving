package ai.konduit.serving.orchestration;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.configprovider.PipelineRouteDefiner;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import org.nd4j.base.Preconditions;
import java.util.List;

@lombok.extern.slf4j.Slf4j
public class ClusteredInferenceVerticle extends BaseRoutableVerticle {


    private InferenceConfiguration inferenceConfiguration;
    private io.vertx.core.spi.cluster.ClusterManager clusterManager;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
    }

    @Override
    public void start() throws Exception {
        super.start();

    }

    @Override
    public void stop() throws Exception {
        super.stop();
        log.debug("Stopping model server.");
    }

    @Override
    public void init(io.vertx.core.Vertx vertx, io.vertx.core.Context context) {
        this.context = context;
        this.vertx = vertx;
        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
            this.router = new PipelineRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
            //get the cluster manager to get node information
            io.vertx.core.impl.VertxImpl impl = (io.vertx.core.impl.VertxImpl) vertx;
            clusterManager = impl.getClusterManager();
            this.router.get("/numnodes").handler(ctx -> {
                List<String> nodes = clusterManager.getNodes();
                ctx.response().putHeader("Content-Type","application/json");
                ctx.response().end(new JsonObject().put("numnodes",nodes.size()).toBuffer());
            });

            this.router.get("/nodes").handler(ctx -> {
                List<String> nodes = clusterManager.getNodes();
                ctx.response().putHeader("Content-Type","application/json");
                ctx.response().end(new JsonObject().put("nodes",new JsonArray(nodes)).toBuffer());
            });

            setupWebServer();
        } catch (java.io.IOException e) {
            log.error("Unable to parse InferenceConfiguration",e);
        }
    }


    protected void setupWebServer() {
       Preconditions.checkNotNull(inferenceConfiguration,"Inference configuration undefined!");
        int portValue = inferenceConfiguration.getServingConfig().getHttpPort();
        if(portValue == 0) {
            String portEnvValue = System.getenv(ai.konduit.serving.verticles.VerticleConstants.PORT_FROM_ENV);
            if(portEnvValue != null) {
                portValue = Integer.parseInt(portEnvValue);
            }
        }

        final int portValueFinal = portValue;
        vertx.createHttpServer()
                .requestHandler(router)
                .exceptionHandler(Throwable::printStackTrace)
                .listen(portValueFinal, inferenceConfiguration.getServingConfig().getListenHost(), listenResult -> {
                    if (listenResult.failed()) {
                        log.debug("Could not start HTTP server");
                        listenResult.cause().printStackTrace();
                    } else {
                        log.debug("Server started on port " + portValueFinal);
                    }
                });
    }

}