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

package ai.konduit.serving.cli.launcher.command;

import ai.konduit.serving.cli.launcher.KonduitServingLauncher;
import ai.konduit.serving.cli.launcher.LauncherUtils;
import ai.konduit.serving.pipeline.settings.DirectoryFetcher;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.spi.launcher.DefaultCommand;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static ai.konduit.serving.cli.launcher.LauncherUtils.getPidFromServerId;

@Name("metrics")
@Summary("Shows the running metrics for a particular server")
@Description("Prints the calculate metrics for a particular server. Useful for getting a quick insight into the running server.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Shows metrics of a server, named 'my_server': \n" +
        "$ konduit metrics my_server \n" +
        "--------------")
public class MetricsCommand extends DefaultCommand {

    private String id;
    
    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void run() {
        if(LauncherUtils.isProcessExists(id)) {
            Vertx vertx = Vertx.vertx();

            try {
                InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                        FileUtils.readFileToString(new File(DirectoryFetcher.getServersDataDir(),
                                getPidFromServerId(id) + ".data"), StandardCharsets.UTF_8));

                HttpRequest<Buffer> request = WebClient.create(vertx)
                                .head(inferenceConfiguration.port(),
                                        inferenceConfiguration.host(),
                                        "/metrics")
                                .method(HttpMethod.GET);

                        Handler<AsyncResult<HttpResponse<Buffer>>> responseHandler = handler -> {
                            if(handler.succeeded()) {
                                HttpResponse<Buffer> httpResponse = handler.result();
                                int statusCode = httpResponse.statusCode();
                                if(statusCode == 200) {
                                    out.print(handler.result().body());
                                } else {
                                    out.format("Request failed with status code: %s%nDetails: %s%n", statusCode,
                                            handler.result().bodyAsString());
                                }
                            } else {
                                out.format("Failed request.%nExecute '%s logs %s' to find the cause.%n",
                                        ((KonduitServingLauncher) executionContext.launcher()).commandLinePrefix(), id);
                            }

                            vertx.close();
                        };

                        request.send(responseHandler);
            } catch (Exception exception) {
                exception.printStackTrace(out);
                vertx.close();
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }
}