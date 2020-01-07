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

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.memory.enums.DebugMode;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.ServerSocket;

import static java.util.concurrent.TimeUnit.SECONDS;

@NotThreadSafe
public abstract class BaseVerticleTest {

    @Rule
    public Timeout timeout = new Timeout(240, SECONDS);

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    protected Vertx vertx;
    protected int port, pubsubPort;
    protected HttpServer httpServer, normalServer;
    protected TestContext context;
    protected Verticle verticle;

    @Before
    public void before(TestContext context) throws Exception {
        port = getRandomPort();
        pubsubPort = getRandomPort();
        System.setProperty("vertx.options.maxEventLoopExecuteTime", "240000");
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setMaxEventLoopExecuteTime(240000);
        vertx = Vertx.vertx(vertxOptions);
        Nd4j.getWorkspaceManager().setDebugMode(DebugMode.SPILL_EVERYTHING);
        setupVertx(vertx);
        if (isPubSub()) {
            httpServer = vertx.createHttpServer().requestHandler(getRequest());
            httpServer.listen(pubsubPort);
        }

        vertx.exceptionHandler(context.exceptionHandler());

        Nd4j.getRandom().setSeed(42);

        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true).setInstances(1)
                .setWorkerPoolSize(1)
                .setConfig(getConfigObject());

        vertx.registerVerticleFactory(new VerticleFactory() {

            @Override
            public String prefix() {
                String[] split = getVertexName().split("\\.");
                return split[split.length -1];
            }

            @Override
            public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
                verticle = (Verticle) classLoader.loadClass(verticleName).newInstance();
                return verticle;
            }
        });

        vertx.deployVerticle(getVertexName(), options, context.asyncAssertSuccess());
    }

    private String getVertexName() {
        return getVerticalClazz().getName();
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        if (httpServer != null)
            httpServer.close();
        if (normalServer != null)
            normalServer.close();
    }

    public Class<? extends AbstractVerticle> getVerticalClazz() {
        return ai.konduit.serving.verticles.inference.InferenceVerticle.class;
    }

    public int getRandomPort() throws IOException {
        ServerSocket pubSubSocket = new ServerSocket(0);
        int ret = pubSubSocket.getLocalPort();
        pubSubSocket.close();
        return ret;
    }

    public abstract Handler<HttpServerRequest> getRequest();

    public abstract JsonObject getConfigObject() throws Exception;

    public void setupVertx(Vertx vertx) { }

    public boolean isPubSub() {
        return false;
    }

}
