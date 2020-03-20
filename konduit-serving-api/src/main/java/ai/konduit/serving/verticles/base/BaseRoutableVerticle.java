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

package ai.konduit.serving.verticles.base;

import ai.konduit.serving.verticles.Routable;
import ai.konduit.serving.verticles.VerticleConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * A super class containing a router
 * and boiler plate methods for managing
 * http interaction.
 *
 * @author Adam Gibson
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public abstract class BaseRoutableVerticle extends AbstractVerticle implements Routable {

    private final static int DEFAULT_HTTP_PORT = 0; // 0 will find an available port when running HttpServer#listen
    protected Router router;
    protected int port;

    public BaseRoutableVerticle() {
        super();
    }

    /**
     * Start an http server the port with the value configured
     * as the httpPort key found in {@link #config()}
     */
    protected void setupWebServer(Promise<Void> startPromise) {
        RouterImpl router = (RouterImpl) router();

        if (context != null && config().containsKey(VerticleConstants.HTTP_PORT_KEY)) {
            String portKey = config().getValue(VerticleConstants.HTTP_PORT_KEY).toString();
            port = Integer.parseInt(portKey);
        } else {
            port = DEFAULT_HTTP_PORT;
            log.warn("No port defined in configuration! Using default port = " + port);
        }

        vertx.createHttpServer()
                .requestHandler(router)
                .exceptionHandler(Throwable::printStackTrace)
                .listen(port, listenResult -> {
                    if (listenResult.failed()) {
                        log.error("Could not start HTTP server", listenResult.cause());
                        startPromise.fail(listenResult.cause());
                    } else {
                        log.info("Server started on port {}", port);
                        startPromise.complete();
                    }
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        setupWebServer(startPromise);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (vertx != null) {
            vertx.close(handler -> {
                if(handler.succeeded()) {
                    log.debug("Shut down server.");
                    stopPromise.complete();
                } else {
                    stopPromise.fail(handler.cause());
                }
            });
        } else {
            stopPromise.complete();
        }
    }

    @Override
    public Router router() {
        return router;
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }
}
