/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.deploy;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.launcher.LauncherUtils;
import ai.konduit.serving.util.ObjectMappers;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.*;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.extern.slf4j.Slf4j;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DeployKonduitServing {

    static {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        LauncherUtils.setCommonLoggingAndVertxProperties();
    }

    public static void deployInference(DeploymentOptions deploymentOptions, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        deployInference(new VertxOptions().setMaxEventLoopExecuteTime(120).setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS),
                deploymentOptions, eventHandler);
    }

    public static void deployInference(InferenceConfiguration inferenceConfiguration, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true));

        log.info("Setup micro meter options.");
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        deployInference(new VertxOptions()
                        .setMetricsOptions(micrometerMetricsOptions)
                        .setMaxEventLoopExecuteTime(120)
                        .setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS),
                new DeploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson())), eventHandler);
    }

    public static void deployInference(VertxOptions vertxOptions, InferenceConfiguration inferenceConfiguration, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        deployInference(vertxOptions, new DeploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson())), eventHandler);
    }

    public static void deployInference(VertxOptions vertxOptions, DeploymentOptions deploymentOptions, Handler<AsyncResult<InferenceConfiguration>> eventHandler) {
        Vertx vertx = Vertx.vertx(vertxOptions);

        vertx.deployVerticle(clazz(), deploymentOptions, handler -> {
            if (handler.failed()) {
                log.error("Unable to deploy verticle {}", className(), handler.cause());

                if (eventHandler != null) {
                    eventHandler.handle(Future.failedFuture(handler.cause()));
                }

                vertx.close();
            } else {
                log.info("Deployed verticle {}", className());
                if (eventHandler != null) {
                    VertxImpl vertxImpl = (VertxImpl) vertx;
                    DeploymentOptions inferenceDeploymentOptions = vertxImpl.getDeployment(handler.result()).deploymentOptions();

                    try {
                        InferenceConfiguration inferenceConfiguration = ObjectMappers.fromJson(inferenceDeploymentOptions.getConfig().encode(), InferenceConfiguration.class);
                        eventHandler.handle(Future.succeededFuture(inferenceConfiguration));
                    } catch (Exception exception){
                        log.debug("Unable to parse json configuration into an InferenceConfiguration object. " +
                                "This can be ignored if the verticle isn't an InferenceVerticle.", exception); // TODO: this is done for supporting other verticles in the future. For instance, 'ConverterVerticle'.
                        eventHandler.handle(Future.succeededFuture());
                    }
                }
            }
        });
    }

    private static Class<? extends Verticle> clazz() {
        return InferenceVerticle.class;
    }

    private static String className() {
        return clazz().getCanonicalName();
    }
}