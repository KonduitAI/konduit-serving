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

package ai.konduit.serving.vertx.verticle;

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class InferenceVerticle extends AbstractVerticle {

    protected ai.konduit.serving.pipeline.api.context.Context pipelineContext;
    protected InferenceConfiguration inferenceConfiguration;
    protected Pipeline pipeline;
    protected PipelineExecutor pipelineExecutor;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

        inferenceConfiguration = InferenceConfiguration.fromJson(context.config().encode());
        pipeline = inferenceConfiguration.getPipeline();
        pipelineExecutor = pipeline.executor();

        log.info("\n\n" +
                "██   ██  ██████  ███    ██ ██████  ██    ██ ██ ████████   ██   ██    ██   ██    \n" +
                "██  ██  ██    ██ ████   ██ ██   ██ ██    ██ ██    ██      ██  ██     ██  ██     \n" +
                "█████   ██    ██ ██ ██  ██ ██   ██ ██    ██ ██    ██      █████      █████      \n" +
                "██  ██  ██    ██ ██  ██ ██ ██   ██ ██    ██ ██    ██      ██  ██     ██  ██     \n" +
                "██   ██  ██████  ██   ████ ██████   ██████  ██    ██      ██   ██ ██ ██   ██ ██ \n");

        log.info("Pending server start, please wait...");
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

    protected long getPid() {
        return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    protected void saveInspectionDataIfRequired(long pid) {
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
