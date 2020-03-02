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

package ai.konduit.serving.configprovider;

import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import ai.konduit.serving.verticles.inference.InferenceVerticle;
import com.beust.jcommander.JCommander;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

/**
 * Single node serving setup using
 * {@link KonduitServingNodeConfigurer}
 * for a single node {@link Vertx}
 * instance
 *
 * @author Adam Gibson
 */
@AllArgsConstructor
@Builder
@Data
public class KonduitServingMain {

    private static Logger log = LoggerFactory.getLogger(KonduitServingMain.class.getName());
    private KonduitServingMainOnSuccessRunnable onSuccess;
    private KonduitServingMainOnFailureRunnable onFailure;

    static {
        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx
    }

    public KonduitServingMain() {
    }

    public static void main(String... args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> log.debug("Shutting down model server.")));
            new KonduitServingMain().runMain(args);
            log.debug("Exiting model server.");
        } catch (Exception e) {
            log.error("Unable to start model server.", e);
            throw e;
        }
    }

    public void runMain(String... args) {
        log.debug("Parsing args " + Arrays.toString(args));
        KonduitServingNodeConfigurer konduitServingNodeConfigurer = new KonduitServingNodeConfigurer();
        //ensure clustering is off
        konduitServingNodeConfigurer.setClustered(false);
        JCommander jCommander = new JCommander(konduitServingNodeConfigurer);
        try {
            jCommander.parse(args);
            if (konduitServingNodeConfigurer.isHelp()) {
                jCommander.usage();
            } else {
                konduitServingNodeConfigurer.setupVertxOptions();
                runMain(konduitServingNodeConfigurer);
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            jCommander.usage();
        }
    }

    public void runMain(KonduitServingNodeConfigurer konduitServingNodeConfigurer) {
        //no need to configure, inference verticle exists
        if (konduitServingNodeConfigurer.getInferenceConfiguration() != null) {
            konduitServingNodeConfigurer.configureWithJson(new JsonObject(konduitServingNodeConfigurer.getInferenceConfiguration().toJson()));

            Vertx vertx = Vertx.vertx(konduitServingNodeConfigurer.getVertxOptions());
            deployVerticle(konduitServingNodeConfigurer, vertx);
        } else {
            Vertx vertx = Vertx.vertx(konduitServingNodeConfigurer.getVertxOptions());
            final ConfigRetriever configRetriever = konduitServingNodeConfigurer.getConfigRetrieverOptions() != null ?
                    ConfigRetriever.create(vertx, konduitServingNodeConfigurer.getConfigRetrieverOptions()) :
                    ConfigRetriever.create(vertx);
            configRetriever.getConfig(result -> {
                if (result.failed()) {
                    log.error("Unable to retrieve configuration " + result.cause());

                    if (onFailure != null) {
                        onFailure.run(result.cause());
                    }
                } else {
                    configRetriever.close(); // We don't need the config retriever to periodically scan for config after it is successfully retrieved.

                    JsonObject json = result.result();
                    konduitServingNodeConfigurer.configureWithJson(json);
                    deployVerticle(konduitServingNodeConfigurer, vertx);
                }
            });
        }
    }

    private void deployVerticle(KonduitServingNodeConfigurer konduitServingNodeConfigurer, Vertx vertx) {
        vertx.deployVerticle(konduitServingNodeConfigurer.getVerticleClassName(), konduitServingNodeConfigurer.getDeploymentOptions(), handler -> {
            if (handler.failed()) {
                log.error(String.format("Unable to deploy verticle %s", konduitServingNodeConfigurer.getVerticleClassName()), handler.cause());

                if (onFailure != null) {
                    onFailure.run(handler.cause());
                }

                vertx.close();
            } else {
                log.info(String.format("Deployed verticle %s", konduitServingNodeConfigurer.getVerticleClassName()));
                if (onSuccess != null) {
                    VertxImpl vertxImpl = (VertxImpl) vertx;
                    Verticle verticle = vertxImpl.getDeployment(handler.result())
                            .getVerticles().iterator().next();

                    if(verticle instanceof BaseRoutableVerticle) {
                        onSuccess.run(((InferenceVerticle) verticle).getPort());
                    } else {
                        onSuccess.run(-1);
                    }
                }
            }
        });
    }

    @FunctionalInterface
    public interface KonduitServingMainOnSuccessRunnable {
        void run(int verticleSelectedPort);
    }

    @FunctionalInterface
    public interface KonduitServingMainOnFailureRunnable {
        void run(Throwable cause);
    }
}