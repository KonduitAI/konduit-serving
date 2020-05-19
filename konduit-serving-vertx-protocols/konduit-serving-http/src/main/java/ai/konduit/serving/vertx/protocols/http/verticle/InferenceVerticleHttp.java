/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.vertx.protocols.http.verticle;

import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.protocols.http.api.ErrorResponse;
import ai.konduit.serving.vertx.protocols.http.api.HttpApiErrorCode;
import ai.konduit.serving.vertx.protocols.http.api.InferenceHttpApi;
import ai.konduit.serving.vertx.protocols.http.api.KonduitServingHttpException;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import ai.konduit.serving.vertx.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
public class InferenceVerticleHttp extends InferenceVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        int port;

        String portEnvValue = System.getenv(EnvironmentConstants.KONDUIT_SERVING_PORT);
        if (portEnvValue != null) {
            try {
                port = Integer.parseInt(portEnvValue);
            } catch (NumberFormatException exception) {
                log.error("Environment variable \"{}={}\" isn't a valid port number.",
                        EnvironmentConstants.KONDUIT_SERVING_PORT, portEnvValue);
                startPromise.fail(exception);
                return;
            }
        } else {
            port = inferenceConfiguration.getPort();
        }

        if (port < 0 || port > 0xFFFF) {
            startPromise.fail(new Exception("Valid port range is 0 <= port <= 65535. The given port was " + port));
            return;
        }

        vertx.createHttpServer()
                .requestHandler(createRouter())
                .exceptionHandler(Throwable::printStackTrace)
                .listen(port, inferenceConfiguration.getHost(), handler -> {
                    if (handler.failed()) {
                        log.error("Could not start HTTP server");
                        startPromise.fail(handler.cause());
                    } else {
                        int actualPort = handler.result().actualPort();
                        inferenceConfiguration.setPort(actualPort);

                        try {
                            ((ContextInternal) context).getDeployment()
                                    .deploymentOptions()
                                    .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                            log.info("Inference HTTP server is listening on host: '{}'", inferenceConfiguration.getHost());
                            log.info("Inference HTTP server started on port {} with {} pipeline steps", actualPort, pipeline.size());
                            startPromise.complete();
                        } catch (Exception exception) {
                            startPromise.fail(exception);
                        }
                    }
                });
    }

    public Router createRouter() {
        InferenceHttpApi inferenceHttpApi = new InferenceHttpApi(pipelineExecutor);

        Router inferenceRouter = Router.router(vertx);

        inferenceRouter.post().handler(BodyHandler.create()
                .setUploadsDirectory(DirectoryFetcher.getFileUploadsDir().getAbsolutePath())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandler -> {
                    Throwable throwable = failureHandler.failure();
                    int statusCode = failureHandler.statusCode();

                    if (statusCode == 404) {
                        log.warn("404 at route " + failureHandler.request().path());
                    } else if (failureHandler.failed()) {
                        if (throwable != null) {
                            log.error("Request failed with cause ", throwable);
                        } else {
                            log.error("Request failed with unknown cause.");
                        }
                    }

                    if(throwable instanceof KonduitServingHttpException) {
                        sendErrorResponse(failureHandler, ((KonduitServingHttpException) throwable).getErrorResponse());
                    } else {
                        failureHandler.response()
                                .setStatusCode(500)
                                .end(throwable != null ? throwable.toString() : "Internal Server Exception");
                    }
                });

        inferenceRouter.post("/predict")
                .consumes(APPLICATION_JSON.toString())
                .consumes(APPLICATION_OCTET_STREAM.toString())
                .produces(APPLICATION_JSON.toString())
                .produces(APPLICATION_OCTET_STREAM.toString())
                .handler(inferenceHttpApi::predict);

        return inferenceRouter;
    }

    private void sendErrorResponse(RoutingContext ctx, ErrorResponse errorResponse) {
        sendErrorResponse(ctx, errorResponse.getErrorCode(), errorResponse.getErrorMessage());
    }

    private void sendErrorResponse(RoutingContext ctx, HttpApiErrorCode errorCode, String errorMessage) {
        ctx.response()
                .setStatusCode(500)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                .end(ObjectMappers.toJson(ErrorResponse.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build()));
    }
}
