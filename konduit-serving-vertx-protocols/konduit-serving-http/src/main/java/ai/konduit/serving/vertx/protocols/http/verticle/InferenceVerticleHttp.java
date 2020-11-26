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

import ai.konduit.serving.endpoint.Endpoint;
import ai.konduit.serving.endpoint.HttpEndpoints;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.metrics.MetricsProvider;
import ai.konduit.serving.pipeline.registry.MicrometerRegistry;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.protocols.http.api.ErrorResponse;
import ai.konduit.serving.vertx.protocols.http.api.HttpApiErrorCode;
import ai.konduit.serving.vertx.protocols.http.api.InferenceHttpApi;
import ai.konduit.serving.vertx.protocols.http.api.KonduitServingHttpException;
import ai.konduit.serving.pipeline.settings.DirectoryFetcher;
import ai.konduit.serving.pipeline.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import com.google.common.base.Strings;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
public class InferenceVerticleHttp extends InferenceVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.executeBlocking(handler -> {
            try {
                initialize();
                handler.complete();
            } catch (Exception exception) {
                handler.fail(exception);
                startPromise.fail(exception);
            }

        }, resultHandler -> {
            if(resultHandler.failed()) {
                if(resultHandler.cause() != null)
                    startPromise.fail(resultHandler.cause());
                else {
                    startPromise.fail("Failed to start. Unknown cause.");
                }
            }
            else {

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
                    port = inferenceConfiguration.port();
                }

                if (port < 0 || port > 0xFFFF) {
                    startPromise.fail(new Exception("Valid port range is 0 <= port <= 65535. The given port was " + port));
                    return;
                }

                vertx.createHttpServer(createOptions(inferenceConfiguration.port(),
                        inferenceConfiguration.useSsl(),
                        inferenceConfiguration.sslKeyPath(),
                        inferenceConfiguration.sslCertificatePath()))
                        .requestHandler(createRouter())
                        .exceptionHandler(throwable -> log.error("Error occurred during http request.", throwable))
                        .listen(port, inferenceConfiguration.host(), handler -> {
                            if (handler.failed()) {
                                startPromise.fail(handler.cause());
                            } else {
                                int actualPort = handler.result().actualPort();
                                inferenceConfiguration.port(actualPort);

                                try {
                                    ((ContextInternal) context).getDeployment()
                                            .deploymentOptions()
                                            .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                                    long pid = getPid();

                                    saveInspectionDataIfRequired(pid);

                                    log.info("Inference HTTP server is listening on host: '{}'", inferenceConfiguration.host());
                                    log.info("Inference HTTP server started on port {} with {} pipeline steps", actualPort, pipeline.size());
                                    startPromise.complete();
                                } catch (Throwable throwable) {
                                    startPromise.fail(throwable);
                                }
                            }
                        });
            }
        });

    }

    private HttpServerOptions createOptions(int port, boolean useSsl, String sslKeyPath, String sslCertificatePath) {
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setPort(port)
                .setHost("0.0.0.0")
                .setSslHandshakeTimeout(0)
                .setCompressionSupported(true)
                .setTcpKeepAlive(true)
                .setTcpNoDelay(true)
                .setAlpnVersions(Arrays.asList(HttpVersion.HTTP_1_0,HttpVersion.HTTP_1_1))
                .setUseAlpn(false)
                .setSsl(useSsl);

        if (useSsl) {
            if (Strings.isNullOrEmpty(sslKeyPath) || Strings.isNullOrEmpty(sslCertificatePath)) {
                if (Strings.isNullOrEmpty(sslKeyPath)) {
                    log.warn("No pem key file specified for SSL.");
                }

                if (Strings.isNullOrEmpty(sslCertificatePath)) {
                    log.warn("No pem certificate file specified for SSL.");
                }

                log.info("Using an auto generated self signed pem key and certificate with SSL.");
                httpServerOptions.setKeyCertOptions(SelfSignedCertificate.create().keyCertOptions());
            } else {
                sslKeyPath = new File(sslKeyPath).getAbsolutePath();
                sslCertificatePath = new File(sslCertificatePath).getAbsolutePath();
                log.info("Using SSL with PEM Key: {} and certificate {}.", sslKeyPath, sslCertificatePath);

                httpServerOptions.setPemKeyCertOptions(new PemKeyCertOptions().setKeyPath(sslKeyPath).setCertPath(sslCertificatePath));
            }
        }

        return httpServerOptions;
    }

    public Router createRouter() {
        InferenceHttpApi inferenceHttpApi = new InferenceHttpApi(pipelineExecutor);

        Router inferenceRouter = Router.router(vertx);
        ServiceLoader<MetricsProvider> sl = ServiceLoader.load(MetricsProvider.class);
        Iterator<MetricsProvider> iterator = sl.iterator();
        MetricsProvider metricsProvider = null;
        if (iterator.hasNext()) {
            metricsProvider = iterator.next();
        }

        Object endpoint = metricsProvider == null ? null : metricsProvider.getEndpoint();
        if (endpoint != null) {
            log.info("MetricsProvider implementation detected, adding endpoint /metrics");
            MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                    .setMicrometerRegistry(MicrometerRegistry.getRegistry())
                    .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
            BackendRegistries.setupBackend(micrometerMetricsOptions);

            inferenceRouter.get("/metrics").handler((Handler<RoutingContext>) endpoint)
                    .failureHandler(failureHandler -> {
                        if (failureHandler.failure() != null) {
                            log.error("Failed to scrape metrics", failureHandler.failure());
                        }

                        failureHandler.response()
                                .setStatusCode(500)
                                .end(failureHandler.failure().toString());
                    });
        }

        inferenceRouter.post().handler(BodyHandler.create()
                .setUploadsDirectory(DirectoryFetcher.getFileUploadsDir().getAbsolutePath())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandler -> {
                    Throwable throwable = failureHandler.failure();
                    int statusCode = failureHandler.statusCode();

                    if (statusCode == 404) {
                        log.warn("404 at route {}" + failureHandler.request().path());
                    } else if (failureHandler.failed()) {
                        if (throwable != null) {
                            log.error("Request failed with cause ", throwable);
                        } else {
                            log.error("Request failed with unknown cause.");
                        }
                    }

                    if (throwable instanceof KonduitServingHttpException) {
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


        //Custom endpoints:
        if (inferenceConfiguration.customEndpoints() != null && !inferenceConfiguration.customEndpoints().isEmpty()) {
            addCustomEndpoints(inferenceHttpApi, inferenceRouter);
        }

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

    private void addCustomEndpoints(InferenceHttpApi inferenceHttpApi, Router inferenceRouter) {
        List<String> e = inferenceConfiguration.customEndpoints();
        PipelineExecutor pe = inferenceHttpApi.getPipelineExecutor();
        Pipeline p = pe.getPipeline();
        for (String s : e) {
            //TODO this won't work for OSGi!
            Class<?> c;
            try {
                c = Class.forName(s);
            } catch (ClassNotFoundException ex) {
                log.error("Error loading custom endpoint for class {}: class not found. Skipping this endpoint", s, ex);
                continue;
            }

            if (!HttpEndpoints.class.isAssignableFrom(c)) {
                log.error("Error loading custom endpoint for class {}: class does not implement ai.konduit.serving.endpoint.HttpEndpoint. Skipping this endpoint", s);
                continue;
            }

            HttpEndpoints h;
            try {
                h = (HttpEndpoints) c.getConstructor().newInstance();
            } catch (NoSuchMethodException | SecurityException ex) {
                log.error("Error loading custom endpoint for class {}: no zero-arg contsructor was found/accessible. Skipping this endpoint", s, ex);
                continue;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                log.error("Error loading custom endpoint for class {}: error creating new instance of class. Skipping this endpoint", s, ex);
                continue;
            }

            List<Endpoint> endpoints;
            try {
                endpoints = h.endpoints(p, pe);
            } catch (Throwable t) {
                log.error("Error loading custom endpoint for class {}: error getting endpoints via HttpEndpoint.endpoints(Pipeline, PipelineExecutor). Skipping this endpoint", s, t);
                continue;
            }

            if (endpoints != null && !endpoints.isEmpty()) {  //May be null/empty if endpoint is pipeline-specific, and not applicable for this pipeline
                for (Endpoint ep : endpoints) {
                    try {
                        String path = ep.path();
                        if (!path.startsWith("/"))
                            path = "/" + path;
                        Route r = inferenceRouter.route(ep.type(), path);
                        if (ep.consumes() != null && !ep.consumes().isEmpty()) {
                            for (String consume : ep.consumes()) {
                                r.consumes(consume);
                            }
                        }

                        if (ep.produces() != null && !ep.produces().isEmpty()) {
                            for (String produces : ep.produces()) {
                                r.produces(produces);
                            }
                        }

                        r.handler(ep.handler());
                    } catch (Throwable t) {
                        log.error("Error loading custom endpoint for class {}: error creating route in Vert.x. Skipping this endpoint: {}", s, ep, t);
                    }
                }
            }
        }
    }
}
