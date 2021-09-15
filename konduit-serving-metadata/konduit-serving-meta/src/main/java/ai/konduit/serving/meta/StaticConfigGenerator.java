/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.meta;

import ai.konduit.serving.util.ObjectMappers;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class StaticConfigGenerator {
    public static void main(String[] args) throws IOException {
        String outputPath = args[0];
        if (outputPath == null || outputPath.isEmpty()) {
            System.err.println("'outputPath' is undefined or empty. Usage: <mainClass> <outputPath>");
            System.exit(1);
        } else {
            File jsonMappingResource = new ClassPathResource("META-INF/konduit-serving/JsonNameMapping").getFile();

            Map<String, String> outputConfigMap =
                    Arrays.stream(FileUtils.readFileToString(jsonMappingResource, StandardCharsets.UTF_8).split("\n"))
                            .map(line -> line.split(","))
                            .filter(splits -> splits[2].equals("ai.konduit.serving.pipeline.api.step.PipelineStep"))
                            .collect(Collectors.toMap(splits -> splits[0],
                                    splits -> {
                                        try {
                                            return ObjectMappers.toJson(Class.forName(splits[1]).getConstructor().newInstance());
                                        } catch (Exception exception) {
                                            System.err.format("Unable to create config for: %s%n%s%n", splits[1], exception);
                                            System.exit(1);
                                            return null;
                                        }
                                    }));

            FileUtils.writeStringToFile(new File(outputPath), ObjectMappers.toJson(outputConfigMap), StandardCharsets.UTF_8);
        }
    }
}
