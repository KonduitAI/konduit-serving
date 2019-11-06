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

package ai.konduit.serving.orchestration;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.configprovider.PipelineRouteDefiner;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.Vertx;
import io.vertx.core.Context;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.spi.cluster.ClusterManager;

import org.nd4j.base.Preconditions;
import java.util.List;

/**
 * Multi node version of {@link ai.konduit.serving.verticles.inference.InferenceVerticle}.
 * Uses {@link ai.konduit.serving.configprovider.KonduitServingNodeConfigurer}
 * to handle logic of loading a pipeline from a {@link InferenceConfiguration}
 * and adds additional capabilities on top such as a {@link io.vertx.core.spi.cluster.ClusterManager}
 * awareness allowing for HA, load balancing, and other capabilities
 *
 * @author Adam Gibson
 */
@lombok.extern.slf4j.Slf4j
public class ClusteredInferenceVerticle extends BaseRoutableVerticle {


    private InferenceConfiguration inferenceConfiguration;
    private ClusterManager clusterManager;

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
    public void init(Vertx vertx, Context context) {
        this.context = context;
        this.vertx = vertx;
        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
            //inference endpoints (pipeline execution, loading,..)
            this.router = new PipelineRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
            //get the cluster manager to get node information
            VertxImpl impl = (VertxImpl) vertx;
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