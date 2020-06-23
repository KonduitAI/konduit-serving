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
import ai.konduit.serving.executioner.PipelineExecutioner;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.routers.MemMapRouteDefiner;
import ai.konduit.serving.routers.PipelineRouteDefiner;
import ai.konduit.serving.settings.DirectoryFetcher;
import ai.konduit.serving.util.LogUtils;
import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.bytedeco.systems.global.linux;
import org.bytedeco.systems.global.macosx;
import org.bytedeco.systems.global.windows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
 * @deprecated To be removed - https://github.com/KonduitAI/konduit-serving/issues/298
 */
@Deprecated
@Slf4j
public class InferenceVerticle extends BaseRoutableVerticle {

    private InferenceConfiguration inferenceConfiguration;
    private PipelineRouteDefiner pipelineRouteDefiner;

    @Override
    public void stop() throws Exception {
        super.stop();

        if(pipelineRouteDefiner.getPipelineExecutioner() != null)
            pipelineRouteDefiner.getPipelineExecutioner().close();
        
        log.debug("Stopping konduit server.");
    }

    @Override
    protected void setupWebServer(Promise<Void> startPromise) {
        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());

            pipelineRouteDefiner = new PipelineRouteDefiner();
            this.router = pipelineRouteDefiner.defineRoutes(vertx, inferenceConfiguration);
            //define the memory map endpoints if the user specifies the memory map configuration
            if (inferenceConfiguration.getMemMapConfig() != null) {
                this.router = new MemMapRouteDefiner().defineRoutes(vertx, inferenceConfiguration);
            } else {
                this.router = pipelineRouteDefiner.defineRoutes(vertx, inferenceConfiguration);

                // Checking if the configuration runners can be created without problems or not
                for (PipelineStep pipelineStep : inferenceConfiguration.getSteps())
                    pipelineStep.createRunner();
            }
        } catch (Exception exception) {
            startPromise.fail(exception);
            return;
        }

        if(inferenceConfiguration.getServingConfig().createLoggingEndpoints()) {
            LogUtils.setFileAppenderIfNeeded();
        }

        String portEnvValue = System.getenv(VerticleConstants.KONDUIT_SERVING_PORT);
        if (portEnvValue != null) {
            try {
                port = Integer.parseInt(portEnvValue);
            } catch (NumberFormatException exception) {
                log.error("Environment variable \"{}={}\" isn't a valid port number.", VerticleConstants.KONDUIT_SERVING_PORT, portEnvValue);
                startPromise.fail(exception);
                return;
            }
        } else {
            port = inferenceConfiguration.getServingConfig().httpPort();
        }

        if (port < 0 || port > 0xFFFF) {
            startPromise.fail(new Exception("Valid port range is 0 <= port <= 65535. The given port was " + port));
            return;
        }

        List<PipelineStep> steps = inferenceConfiguration.getSteps();
        final int nSteps = steps == null ? 0 : steps.size();
        vertx.createHttpServer()
                .requestHandler(router)
                .exceptionHandler(Throwable::printStackTrace)
                .listen(port, inferenceConfiguration.getServingConfig().listenHost(), handler -> {
                    if (handler.failed()) {
                        log.error("Could not start HTTP server");
                        startPromise.fail(handler.cause());
                    } else {
                        port = handler.result().actualPort();
                        inferenceConfiguration.getServingConfig().httpPort(port);

                        try {
                            ((ContextInternal) context).getDeployment().deploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson()));

                            int pid = getPid();

                            if(pid != -1) {
                                saveInspectionDataIfRequired(pid);

                                // Periodically checks for configuration updates and save them.
                                vertx.setPeriodic(10000, periodicHandler -> saveInspectionDataIfRequired(pid));
                            }

                            log.info("Inference server is listening on host: '{}'", inferenceConfiguration.getServingConfig().listenHost());
                            log.info("Inference server started on port {} with {} pipeline steps", port, nSteps);
                            startPromise.complete();
                        } catch (Exception exception) {
                            startPromise.fail(exception);
                        }
                    }
                });
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