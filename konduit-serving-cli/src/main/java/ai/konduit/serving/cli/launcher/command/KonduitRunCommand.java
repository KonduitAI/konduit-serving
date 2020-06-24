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

package ai.konduit.serving.cli.launcher.command;

import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import ai.konduit.serving.vertx.api.DeployKonduitServing;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.impl.launcher.CommandLineUtils;
import io.vertx.core.impl.launcher.commands.RunCommand;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static ai.konduit.serving.vertx.api.DeployKonduitServing.INFERENCE_SERVICE_IDENTIFIER;

@Slf4j
@Name(value = "run", priority = 1)
@Summary("Runs a konduit server in the foreground.")
@Description("Runs a konduit server in the foreground.")
public class KonduitRunCommand extends RunCommand {

    public static final String INFERENCE_SERVICE_TYPE_NAME = "inference";
    public static final String DEFAULT_SERVICE = INFERENCE_SERVICE_TYPE_NAME;
    public static final List<String> VALID_SERVICE_TYPES = Collections.singletonList(INFERENCE_SERVICE_TYPE_NAME);
    private String serviceType;
    private InferenceConfiguration inferenceConfiguration;

    @Override
    @Option(longName = "service", shortName = "s", argName = "service-type")
    @DefaultValue(DEFAULT_SERVICE)
    @Description("Service type that needs to be deployed. Defaults to '" + DEFAULT_SERVICE + "'")
    public void setMainVerticle(String serviceType) {
        if(VALID_SERVICE_TYPES.contains(serviceType)) {
            this.serviceType = serviceType;
        } else {
            throw new CLIException(
                    String.format("Invalid service type %s. Allowed values are: %s",
                    serviceType,
                    VALID_SERVICE_TYPES)
            );
        }
    }

    /**
     * The main verticle configuration, it can be a json file or a json string.
     *
     * @param configuration the configuration
     */
    @Override
    @Option(shortName = "c", longName = "config", argName = "config", required = true)
    @Description("Specifies a configuration that should be provided to the verticle. <config> should reference either a " +
            "text file containing a valid JSON object which represents the configuration OR be a JSON string.")
    public void setConfig(String configuration) {
        File file = new File(configuration);
        if(file.exists()) {
            configuration = file.getAbsolutePath();
        }

        log.info("Processing configuration: {}", configuration);
        super.setConfig(configuration);
    }

    @Override
    public void run() {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        String serverId = getServerId();
        log.info("Starting konduit server with an id of '{}'", serverId);

        super.run();
    }

    private String getServerId() {
        String[] commandSplits = CommandLineUtils.getCommand().split(" ");
        String lastSegment = commandSplits[commandSplits.length - 1];
        if(lastSegment.contains("serving.id")) {
            return lastSegment.replace("-Dserving.id=", "").trim();
        } else {
            return null;
        }
    }

    @Override
    protected JsonObject getJsonFromFileOrString(String jsonOrYamlFileOrString, String argName) {
        if (jsonOrYamlFileOrString != null) {
            File scanFile = null;
            try {
                scanFile = URIResolver.isUrl(jsonOrYamlFileOrString) ?
                        URIResolver.getFile(jsonOrYamlFileOrString) :
                        new File(jsonOrYamlFileOrString);
            } catch (IOException e) {
                log.error("Failed to load model " + jsonOrYamlFileOrString, e);
                return null;
            }

            try (Scanner scanner = new Scanner(scanFile, "UTF-8").useDelimiter("\\A")) {
                return readConfiguration(scanner.next());
            } catch (FileNotFoundException e) {
                return readConfiguration(jsonOrYamlFileOrString);
            }
        } else {
            return null;
        }
    }

    /**
     * Parse the given configuration yaml/json string to {@link JsonObject}. The
     * configuration should be parsable to {@link InferenceConfiguration}.
     *
     * @param configurationString given configuration string
     * @return Read configuration to JsonObject. Returns null on failure.
     */
    private JsonObject readConfiguration(String configurationString) {
        try {
            inferenceConfiguration = InferenceConfiguration.fromJson(configurationString);
            return new JsonObject(inferenceConfiguration.toJson());
        } catch (Exception jsonProcessingErrors) {
            try {
                inferenceConfiguration = InferenceConfiguration.fromYaml(configurationString);
                return new JsonObject(inferenceConfiguration.toJson());
            } catch (Exception yamlProcessingErrors) {
                log.error("Given configuration: {} does not contain a valid JSON/YAML object", configurationString);
                log.error("\n\nErrors while processing as a json string:", jsonProcessingErrors);
                log.error("\n\nErrors while processing as a yaml string:", yamlProcessingErrors);
                return null;
            }
        }
    }

    @Override
    protected void deploy() {
        if (INFERENCE_SERVICE_TYPE_NAME.equalsIgnoreCase(serviceType)) {
            DeployKonduitServing.registerInferenceVerticleFactory(vertx);
            super.setMainVerticle(INFERENCE_SERVICE_IDENTIFIER + ":" + inferenceConfiguration.getProtocol().name().toLowerCase());
        } else {
            throw new CLIException(String.format("Unsupported service type %s", serviceType));
        }

        deploy(mainVerticle, vertx, deploymentOptions, res -> {});
    }
}