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

package ai.konduit.serving.verticles;

import static java.util.concurrent.TimeUnit.SECONDS;

@javax.annotation.concurrent.NotThreadSafe
public abstract class BaseVerticleTest {
    @org.junit.Rule
    public io.vertx.ext.unit.junit.Timeout timeout = new io.vertx.ext.unit.junit.Timeout(240, SECONDS);


    @org.junit.Rule
    public org.junit.rules.TemporaryFolder temporary = new org.junit.rules.TemporaryFolder();

    protected io.vertx.core.Vertx vertx;
    protected int port, pubsubPort;
    protected io.vertx.core.http.HttpServer httpServer, normalServer;
    protected io.vertx.ext.unit.TestContext context;
    protected io.vertx.core.Verticle verticle;

    @org.junit.Before
    public void before(io.vertx.ext.unit.TestContext context) throws Exception {
        port = getRandomPort();
        pubsubPort = getRandomPort();
        System.setProperty("vertx.options.maxEventLoopExecuteTime", "240000");
        io.vertx.core.VertxOptions vertxOptions = new io.vertx.core.VertxOptions();
        vertxOptions.setMaxEventLoopExecuteTime(240000);
        vertx = io.vertx.core.Vertx.vertx(vertxOptions);
        org.nd4j.linalg.factory.Nd4j.getWorkspaceManager().setDebugMode(org.nd4j.linalg.api.memory.enums.DebugMode.SPILL_EVERYTHING);
        setupVertx(vertx);
        if (isPubSub()) {
            httpServer = vertx.createHttpServer().requestHandler(getRequest());
            httpServer.listen(pubsubPort);
        }

        vertx.exceptionHandler(context.exceptionHandler());

        org.nd4j.linalg.factory.Nd4j.getRandom().setSeed(42);

        io.vertx.core.DeploymentOptions options = new io.vertx.core.DeploymentOptions()
                .setWorker(true).setInstances(1)
                .setWorkerPoolSize(1)
                .setConfig(getConfigObject());
        String verticleClassName = getVertexName();
        String[] split = verticleClassName.split("\\.");
        vertx.registerVerticleFactory(new io.vertx.core.spi.VerticleFactory() {
            @Override
            public String prefix() {
                return split[split.length - 1];
            }

            @Override
            public io.vertx.core.Verticle createVerticle(String s, ClassLoader classLoader) throws Exception {
                io.vertx.core.Verticle ret = (io.vertx.core.Verticle) classLoader.loadClass(verticleClassName).newInstance();
                verticle = ret;
                return ret;
            }
        });


        vertx.registerVerticleFactory(new io.vertx.core.spi.VerticleFactory() {
            @Override
            public String prefix() {
                return getVertexName();
            }

            @Override
            public io.vertx.core.Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
                verticle = (io.vertx.core.Verticle) classLoader.loadClass(verticleName).newInstance();
                return verticle;
            }
        });


        vertx.deployVerticle(getVertexName(), options, context.asyncAssertSuccess());
    }

    private String getVertexName() {
        return getVerticalClazz().getName();
    }

    @org.junit.After
    public void after(io.vertx.ext.unit.TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        if (httpServer != null)
            httpServer.close();
        if (normalServer != null)
            normalServer.close();
    }


    public abstract Class<? extends io.vertx.core.AbstractVerticle> getVerticalClazz();

    public int getRandomPort() throws java.io.IOException {
        java.net.ServerSocket pubSubSocket = new java.net.ServerSocket(0);
        int ret = pubSubSocket.getLocalPort();
        pubSubSocket.close();
        return ret;
    }

    public abstract io.vertx.core.Handler<io.vertx.core.http.HttpServerRequest> getRequest();

    public abstract io.vertx.core.json.JsonObject getConfigObject() throws Exception;

    public void setupVertx(io.vertx.core.Vertx vertx) {

    }


    public boolean isPubSub() {
        return false;
    }

}
