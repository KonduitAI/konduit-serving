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

import ai.konduit.serving.vertx.protocols.http.api.InferenceHttpApi;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import ai.konduit.serving.vertx.settings.constants.EnvironmentConstants;
import ai.konduit.serving.vertx.verticle.InferenceVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bytedeco.systems.global.linux;
import org.bytedeco.systems.global.macosx;
import org.bytedeco.systems.global.windows;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;

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
                        HttpServer resultantHttpServer = handler.result();
                        inferenceConfiguration.setPort(resultantHttpServer.actualPort());

                        try {
                            ((ContextInternal) context).getDeployment()
                                    .deploymentOptions()
                                    .setConfig(new JsonObject(inferenceConfiguration.toJson()));

                            int pid = getPid();

                            if(pid != -1) {
                                saveInspectionDataIfRequired(pid);

                                // Periodically checks for configuration updates and save them.
                                vertx.setPeriodic(10000, periodicHandler -> saveInspectionDataIfRequired(pid));
                            }

                            log.info("Inference server is listening on host: '{}'", inferenceConfiguration.getHost());
                            log.info("Inference server started on port {} with {} pipeline steps", port, pipeline.size());
                            startPromise.complete();
                        } catch (Exception exception) {
                            startPromise.fail(exception);
                        }
                    }
                });
    }

    public Router createRouter() {
        InferenceHttpApi inferenceHttpApi = new InferenceHttpApi(pipelineContext, pipelineExecutor);

        Router inferenceRouter = Router.router(vertx);

        inferenceRouter.post().handler(BodyHandler.create()
                .setUploadsDirectory(DirectoryFetcher.getFileUploadsDir().getAbsolutePath())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandlder -> {
                    if (failureHandlder.statusCode() == 404) {
                        log.warn("404 at route " + failureHandlder.request().path());
                    } else if (failureHandlder.failed()) {
                        if (failureHandlder.failure() != null) {
                            log.error("Request failed with cause ", failureHandlder.failure());
                        } else {
                            log.error("Request failed with unknown cause.");
                        }
                    }

                    failureHandlder.response()
                            .setStatusCode(500)
                            .end(failureHandlder.failure().toString());
                });


        inferenceRouter.post("/predict")
                .consumes(APPLICATION_JSON.toString())
                .consumes(APPLICATION_OCTET_STREAM.toString())
                .produces(APPLICATION_JSON.toString())
                .produces(APPLICATION_OCTET_STREAM.toString())
                .handler(inferenceHttpApi::predict);

        return inferenceRouter;
    }

    public static int getPid() throws UnsatisfiedLinkError {
        if (SystemUtils.IS_OS_WINDOWS) {
            return windows.GetCurrentProcessId();
        } else if (SystemUtils.IS_OS_MAC) {
            return macosx.getpid();
        } else if (SystemUtils.IS_OS_LINUX){
            return linux.getpid();
        } else {
            return -1;
        }
    }

    private void saveInspectionDataIfRequired(int pid) {
        try {
            File processConfigFile = new File(DirectoryFetcher.getServersDataDir(), pid + ".data");
            String inferenceConfigurationJson = ((ContextInternal) context).getDeployment()
                    .deploymentOptions().getConfig().encodePrettily();

            if(processConfigFile.exists()) {
                if(!FileUtils.readFileToString(processConfigFile, StandardCharsets.UTF_8).contains(inferenceConfigurationJson)) {
                    FileUtils.writeStringToFile(processConfigFile, inferenceConfigurationJson, StandardCharsets.UTF_8);
                }
            } else {
                FileUtils.writeStringToFile(processConfigFile, inferenceConfigurationJson, StandardCharsets.UTF_8);
                log.info("Writing inspection data at '{}' with configuration: \n{}", processConfigFile.getAbsolutePath(),
                        inferenceConfiguration.toJson());
            }
            processConfigFile.deleteOnExit();
        } catch (IOException exception) {
            log.error("Unable to save konduit server inspection information", exception);
        }
    }
}
