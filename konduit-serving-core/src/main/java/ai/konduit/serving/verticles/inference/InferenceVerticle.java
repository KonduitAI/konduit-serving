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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

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
        log.debug("Stopping konduit server.");
    }

    @Override
    public void init(Vertx vertx, Context context) {
        this.context = context;
        this.vertx = vertx;
        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
            this.router = new PipelineRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
            //define the memory map endpoints if the user specifies the memory map configuration
            if (inferenceConfiguration.getMemMapConfig() != null) {
                this.router = new MemMapRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
            } else {
                this.router = new PipelineRouteDefiner().defineRoutes(vertx, inferenceConfiguration);

                // Checking if the configuration runners can be created without problems or not
                for (PipelineStep pipelineStep : inferenceConfiguration.getSteps())
                    pipelineStep.createRunner();
            }

            setupWebServer();
        } catch (IOException e) {
            log.error("Unable to parse InferenceConfiguration", e);
        }
    }

    protected void setupWebServer() {
        int portValue = inferenceConfiguration.getServingConfig().getHttpPort();
        if (portValue == 0) {
            String portEnvValue = System.getenv(VerticleConstants.PORT_FROM_ENV);
            if (portEnvValue != null) {
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