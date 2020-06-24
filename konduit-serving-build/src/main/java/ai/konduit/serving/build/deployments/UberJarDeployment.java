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
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class UberJarDeployment implements Deployment {
    public static final String DEFAULT_GROUPID = "ai.konduit";
    public static final String DEFAULT_ARTIFACT = "konduit-serving-uberjar";
    public static final String DEFAULT_JAR_NAME = "konduit-serving-deployment.jar";

    public static final String PROP_OUTPUTDIR = "jar.outputdir";
    public static final String PROP_JARNAME = "jar.name";
    public static final String PROP_GID = "jar.groupid";
    public static final String PROP_AID = "jar.artifactid";
    public static final String PROP_VER = "jar.version";

    public static final String CLI_KEYS = "JAR deployment config keys: " + PROP_OUTPUTDIR + ", " + PROP_JARNAME + ","
            + PROP_GID + ", " + PROP_AID + ", " + PROP_VER;

    private String outputDir;
    private String jarName;
    private String groupId;
    private String artifactId;
    private String version;

    public UberJarDeployment(String outputDir){
        this(outputDir, DEFAULT_JAR_NAME, DEFAULT_GROUPID, DEFAULT_ARTIFACT, defaultVersion());
    }

    public UberJarDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("jarName") String jarName,
                             @JsonProperty("groupId") String groupId, @JsonProperty("artifactId") String artifactId,
                             @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.jarName = jarName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    private static String defaultVersion(){
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDD-HHmmss.SSS");
        return sdf.format(new Date(time));
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(PROP_OUTPUTDIR, PROP_JARNAME, PROP_GID, PROP_AID, PROP_VER);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_JARNAME, jarName);
        m.put(PROP_GID, groupId);
        m.put(PROP_AID, artifactId);
        m.put(PROP_VER, version);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> p) {
        outputDir = p.getOrDefault(PROP_OUTPUTDIR, outputDir);
        jarName = p.getOrDefault(PROP_JARNAME, jarName);
        groupId = p.getOrDefault(PROP_GID, groupId);
        artifactId = p.getOrDefault(PROP_AID, artifactId);
        version = p.getOrDefault(PROP_VER, version);
    }

    @Override
    public DeploymentValidation validate() {
        //TODO we need to validate the actual content - not that it's set. i.e., certain characters can't be used
        // for groupid, artifacts, version, jar name, etc
        if(outputDir == null || outputDir.isEmpty()){
            return new SimpleDeploymentValidation("No output directory is set (property: " + PROP_OUTPUTDIR + ")");
        }
        return new SimpleDeploymentValidation();
    }

    @Override
    public String outputString() {
        File outFile = new File(outputDir, jarName);
        StringBuilder sb = new StringBuilder();
        sb.append("JAR location:        ").append(outFile.getAbsolutePath()).append("\n");
        String size;
        String filename = "";
        if(outFile.exists()){
            long bytes = outFile.length();
            double bytesPerMB = 1024 * 1024;
            double mb = bytes / bytesPerMB;
            size = String.format("%.2f", mb) + " MB";
            filename = outFile.getName();
        } else {
            size = "<JAR not found>";
            filename = "<jar file name>";
        }
        sb.append("JAR size:            ").append(size).append("\n");
        sb.append("JAR launch command:  java -jar ").append(filename).append(" <serve|list|stop|inpect|logs>\n");
        return sb.toString();
    }

    @Override
    public List<String> gradleImports() {
        return Collections.singletonList("com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar");
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        return Collections.singletonList(new GradlePlugin("com.github.johnrengelman.shadow", "2.0.4"));
    }

    @Override
    public List<String> gradleTaskNames() {
        return Collections.singletonList("shadowJar");
    }
}
