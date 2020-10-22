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

package ai.konduit.serving.cli;

import ai.konduit.serving.util.ObjectMappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class StaticConfigGenerator {
    public static void main(String[] args) throws IOException {
        String outputPath = args[1];
        if (outputPath == null || outputPath.isEmpty()) {
            log.error("'outputPath' is undefined or empty. Usage: <mainClass> <outputPath>");
            System.exit(1);
        } else {
            File jsonMappingResource = new ClassPathResource("META-INF/konduit-serving/JsonNameMapping").getFile();

            Map<String, String> outputConfigMap =
                    Arrays.stream(FileUtils.readFileToString(jsonMappingResource, StandardCharsets.UTF_8).split(System.lineSeparator()))
                            .map(line -> line.split(","))
                            .filter(splits -> splits[2].equals("ai.konduit.serving.pipeline.api.step.PipelineStep"))
                            .collect(Collectors.toMap(splits -> splits[0],
                                    splits -> {
                                        try {
                                            return ObjectMappers.toJson(Class.forName(splits[1]).getConstructor().newInstance());
                                        } catch (Exception exception) {
                                            log.error("Unable to create config for: {}", splits[1], exception);
                                            System.exit(1);
                                            return null;
                                        }
                                    }));

            FileUtils.writeStringToFile(new File(outputPath), ObjectMappers.toJson(outputConfigMap), StandardCharsets.UTF_8);
        }
    }
}
