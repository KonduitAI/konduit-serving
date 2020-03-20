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

package ai.konduit.serving.verticles.inference;


import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.configprovider.MemMapRouteDefiner;
import ai.konduit.serving.configprovider.PipelineRouteDefiner;
import ai.konduit.serving.executioner.PipelineExecutioner;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * A {@link io.vertx.core.Verticle} that takes multi part file uploads
 * as inputs.
 * <p>
 * Many computation graphs are usually multiple inputs
 * by name: Each part for a multi part file upload
 * should map on to a name. For example:
 * input_1 : part name: input_1
 * input_2 : part name: input_2
 * <p>
 * The handler logic for this {@link io.vertx.core.Verticle} is implemented
 * in {@link PipelineExecutioner}
 *
 * @author Adam Gibson
 */
@Slf4j
public class InferenceVerticle extends BaseRoutableVerticle {

    private InferenceConfiguration inferenceConfiguration;
    private PipelineRouteDefiner pipelineRouteDefiner;

    @Override
    public void stop() throws Exception {
        super.stop();

        if(pipelineRouteDefiner.getPipelineExecutioner() != null)
            pipelineRouteDefiner.getPipelineExecutioner().close();
        
        log.debug("Stopping konduit server.");
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

        inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
        pipelineRouteDefiner = new PipelineRouteDefiner();
        this.router = pipelineRouteDefiner.defineRoutes(vertx, inferenceConfiguration);
        //define the memory map endpoints if the user specifies the memory map configuration
        if (inferenceConfiguration.getMemMapConfig() != null) {
            this.router = new MemMapRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
        } else {
            this.router = pipelineRouteDefiner.defineRoutes(vertx, inferenceConfiguration);

            // Checking if the configuration runners can be created without problems or not
            for (PipelineStep pipelineStep : inferenceConfiguration.getSteps())
                pipelineStep.createRunner();
        }
    }

    @Override
    protected void setupWebServer(Promise<Void> startPromise) {
        String portEnvValue = System.getenv(VerticleConstants.KONDUIT_SERVING_PORT);
        if (portEnvValue != null) {
            try {
                port = Integer.parseInt(portEnvValue);
            } catch (NumberFormatException exception) {
                log.error("Environment variable \"{}={}\" isn't a valid port number.", VerticleConstants.KONDUIT_SERVING_PORT, portEnvValue);
                startPromise.fail(exception);
                return;
            }
        } else {
            port = inferenceConfiguration.getServingConfig().getHttpPort();
        }

        if (port < 0 || port > 0xFFFF) {
            startPromise.fail(new Exception(String.format("Valid port range is 0 <= port <= 65535. The given port was %s", port)));
            return;
        }

        List<PipelineStep> steps = inferenceConfiguration.getSteps();
        final int nSteps = steps == null ? 0 : steps.size();
        vertx.createHttpServer()
                .requestHandler(router)
                .exceptionHandler(Throwable::printStackTrace)
                .listen(port, inferenceConfiguration.getServingConfig().getListenHost(), handler -> {
                    if (handler.failed()) {
                        log.error("Could not start HTTP server");
                        startPromise.fail(handler.cause());
                    } else {
                        port = handler.result().actualPort();
                        inferenceConfiguration.getServingConfig().setHttpPort(port);

                        try {
                            ((ContextInternal) context).getDeployment().deploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson()));

                            log.info("Inference server started on port {} with {} pipeline steps", port, nSteps);
                            startPromise.complete();
                        } catch (Exception exception) {
                            startPromise.fail(exception);
                        }
                    }
                });
    }
}