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

import com.beust.jcommander.JCommander;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;

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
public class KonduitServingMain {

    private static Logger log = LoggerFactory.getLogger(KonduitServingMain.class.getName());
    private Runnable onSuccess;
    private Runnable onFailure;

    static {
        setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx
    }

    public KonduitServingMain() { }

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
        Vertx vertx = Vertx.vertx(konduitServingNodeConfigurer.getVertxOptions());
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, konduitServingNodeConfigurer.getConfigRetrieverOptions());
        configRetriever.getConfig(result -> {
            if (result.failed()) {
                log.error("Unable to retrieve configuration " + result.cause());

                if(onFailure != null) {
                    onFailure.run();
                }
            } else {
                configRetriever.close(); // We don't need the config retriever to periodically scan for config after it is successfully retrieved.

                JsonObject json = result.result();
                konduitServingNodeConfigurer.configureWithJson(json);
              
                vertx.deployVerticle(konduitServingNodeConfigurer.getVerticleClassName(), konduitServingNodeConfigurer.getDeploymentOptions(), handler -> {
                    if (handler.failed()) {
                        log.error(String.format("Unable to deploy verticle %s", konduitServingNodeConfigurer.getVerticleClassName()), handler.cause());
                        if(onFailure != null) {
                            onFailure.run();
                        }

                        vertx.close();
                    } else {
                        log.info(String.format("Deployed verticle %s", konduitServingNodeConfigurer.getVerticleClassName()));
                        if(onSuccess != null) {
                            onSuccess.run();
                        }
                    }
                });
            }
        });
    }
}
