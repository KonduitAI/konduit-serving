/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.*;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class ExeDeployment implements Deployment {
    public static final String DEFAULT_EXE_NAME = "konduit-serving-deployment.exe";
    public static final String PROP_OUTPUTDIR = "exe.outputdir";
    public static final String PROP_EXENAME = "exe.name";

    private String outputDir;
    private String exeName;
    private String version;

    public ExeDeployment(String outputDir) {
        this(outputDir, "ks", Deployment.defaultVersion());
    }

    public ExeDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("exeName") String exeName,
                         @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.exeName = exeName;
        this.version = version;
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(PROP_OUTPUTDIR, PROP_EXENAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_EXENAME, exeName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputDir = props.getOrDefault(PROP_OUTPUTDIR, outputDir);
        exeName = props.getOrDefault(PROP_EXENAME, exeName);
    }

    @Override
    public DeploymentValidation validate() {
        return null;
    }

    @Override
    public String outputString() {
        File outFile = new File(outputDir, exeName);
        StringBuilder sb = new StringBuilder();
        sb.append("EXE location:   ").append(outFile.getAbsolutePath()).append("\n");
        String size;
        if(outFile.exists()){
            long bytes = outFile.length();
            double bytesPerMB = 1024 * 1024;
            double mb = bytes / bytesPerMB;
            size = String.format("%.2f", mb) + " MB";
        } else {
            size = "<EXE not found>";
        }
        sb.append("EXE size:       ").append(size);

        return sb.toString();
    }

    @Override
    public List<String> gradleImports() {
        List<String> retVal = new ArrayList<>();
        retVal.add("edu.sc.seis.launch4j.tasks.DefaultLaunch4jTask");
        retVal.add("com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar");
        return retVal;
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        List<GradlePlugin> retVal = new ArrayList<>();
        retVal.add(new GradlePlugin("nebula.ospackage", "8.3.0"));
        retVal.add(new GradlePlugin("com.github.johnrengelman.shadow", "2.0.4"));
        retVal.add(new GradlePlugin("edu.sc.seis.launch4j", "2.4.6"));
        return retVal;
    }

    @Override
    public List<String> gradleTaskNames() {
        List<String> ret = new ArrayList<>();
        ret.add("shadowJar");
        ret.add("createExe");
        ret.add("copyExe");
        return ret;
    }
}
