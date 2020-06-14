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

import ai.konduit.serving.cli.launcher.LauncherUtils;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.settings.DirectoryFetcher;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.launcher.DefaultCommand;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.guava.base.Strings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("inspect")
@Summary("Inspect the details of a particular konduit server.")
@Description("Inspect the details of a particular konduit server given an id. To find a list of running servers and their details, use the 'list' command.\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Prints the whole inference configuration of server with an id of 'inf_server':\n" +
        "$ konduit inspect inf_server\n\n" +
        "- Queries the inference configuration of server with an id of 'inf_server'\n" +
        "  based on the given pattern and gives output similar to 'localhost:45223'\n" +
        "$ konduit inspect inf_server -q {host}:{port}\n\n" +
        "- Queries the inference configuration of server with an id of 'inf_server'\n" +
        "  based on the given pattern and gives output similar to \n" +
        "  'localhost:45223-{<pipeline_details>}'. The curly brackets can be escaped.\n" +
        "$ konduit inspect inf_server -q {host}:{port}-\\{{pipeline}\\}\n" +
        "--------------")
public class InspectCommand extends DefaultCommand {

    protected static final Pattern QUERY_PATTERN = Pattern.compile("(?<!\\\\)\\{([^}]+.?)(?<!\\\\)}");
    private String id;
    private String query;

    @Argument(index = 0, argName = "server-id")
    @Description("Konduit server id")
    public void setId(String id) {
        this.id = id;
    }

    @Option(longName = "query", shortName = "q", argName = "pattern")
    @Description("Query pattern to inspect. Pattern: {key1}... or {key1.key2[0].key3}... " +
            "See above examples for usage.")
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void run() {
        if(LauncherUtils.isProcessExists(id)) {
            try {
                int pid = LauncherUtils.getPidFromServerId(id);
                InferenceConfiguration inferenceConfiguration = InferenceConfiguration.fromJson(
                        FileUtils.readFileToString(
                            new File(DirectoryFetcher.getServersDataDir(),
                                    pid + ".data"),
                            StandardCharsets.UTF_8));
                String json = inferenceConfiguration.toJson();

                if(Strings.isNullOrEmpty(query)) {
                    out.println(json);
                } else {
                    Matcher matcher = QUERY_PATTERN.matcher(query);
                    String output = query;
                    while(true) {
                        if(matcher.find()) {
                            String key = matcher.group(1);
                            String result;
                            if("pid".equalsIgnoreCase(key)) {
                                result = String.valueOf(pid);
                            } else if ("size".equalsIgnoreCase(key)){
                                result = String.valueOf(inferenceConfiguration.getPipeline().size());
                            } else {
                                Object outputObject = JsonPath.read(json, "$." + matcher.group(1));
                                if(outputObject instanceof LinkedHashMap) {
                                    result = JsonObject.mapFrom(outputObject).encode();
                                } else if (outputObject instanceof List) {
                                    result = new JsonArray((List) outputObject).encode();
                                } else {
                                    result = outputObject.toString();
                                }
                            }
                            output = output.replace(matcher.group(), result);
                        } else {
                            break;
                        }
                    }
                    out.println(output
                            .replace("\\{", "{")
                            .replace("\\}", "}"));
                }
            } catch (Exception exception) {
                exception.printStackTrace(out);
            }
        } else {
            out.println("No konduit server exists with an id: " + id);
        }
    }

}
