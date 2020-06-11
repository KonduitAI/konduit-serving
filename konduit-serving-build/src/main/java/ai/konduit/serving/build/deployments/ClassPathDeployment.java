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

package ai.konduit.serving.build.deployments;

import ai.konduit.serving.build.build.GradlePlugin;
import ai.konduit.serving.build.config.Deployment;
import ai.konduit.serving.build.config.DeploymentValidation;
import ai.konduit.serving.build.config.SimpleDeploymentValidation;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Data
@Accessors(fluent = true)
public class ClassPathDeployment implements Deployment {
    public enum Type {
        TEXT_FILE,
        JAR_MANIFEST
    }

    public static final String OUTPUT_FILE_PROP = "classpath.outputFile";
    public static final String TYPE_PROP = "classpath.type";

    private String outputFile;
    private Type type;

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(OUTPUT_FILE_PROP, TYPE_PROP);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> map = new HashMap<>();
        map.put(OUTPUT_FILE_PROP, outputFile);
        map.put(TYPE_PROP, type == null ? null : type.toString());
        return map;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputFile = props.getOrDefault(OUTPUT_FILE_PROP, outputFile);
        if(props.containsKey(TYPE_PROP)){
            type = Type.valueOf(props.get(TYPE_PROP).toUpperCase());
        }
    }

    @Override
    public DeploymentValidation validate() {
        if (outputFile != null && !outputFile.isEmpty() && type != null) {
            return new SimpleDeploymentValidation();
        }
        List<String> errs = new ArrayList<>();
        if(outputFile == null || outputFile.isEmpty()){
            errs.add("Output classpath file (" + OUTPUT_FILE_PROP + " property) is not set");
        }
        if(type == null){
            errs.add("Output classpath file type - " + Type.TEXT_FILE + " or " + Type.JAR_MANIFEST + " (" + TYPE_PROP + " property) is not set");
        } else if(type == Type.JAR_MANIFEST && outputFile != null && !outputFile.endsWith(".jar")){
            errs.add("Output classpath file (JAR_MANIFEST type) output file name (" + TYPE_PROP + " property) must end with .jar, got \"" + outputFile + "\"");
        }
        return new SimpleDeploymentValidation(errs);
    }

    @Override
    public String outputString() {
        File f = new File(outputFile);
        StringBuilder sb = new StringBuilder();
        sb.append("Classpath file location:        ").append(f.getAbsolutePath()).append("\n");
        String nLines;
        if (f.exists()) {
            try {
                nLines = String.valueOf(FileUtils.readLines(f, StandardCharsets.UTF_8).size());
            } catch (IOException e) {
                nLines = "<Error reading generated classpath file>";
                log.warn("Error reading generated classpath file", e);
            }
        } else {
            nLines = "<output file not found>";
        }
        sb.append("Number of classpath entries:    ").append(nLines).append("\n");
        return sb.toString();
    }

    @Override
    public List<String> gradleImports() {
        return Collections.emptyList();
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        return Collections.emptyList();
    }

    @Override
    public List<String> gradleTaskNames() {
        return Collections.singletonList("build");
    }
}
