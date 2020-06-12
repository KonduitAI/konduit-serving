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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.*;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class DebDeployment implements Deployment {
    public static final String DEFAULT_EXE_NAME = "konduit-serving-deployment.deb";
    public static final String PROP_OUTPUTDIR = "deb.outputdir";
    public static final String PROP_RPMNAME = "deb.name";

    private String outputDir;
    private String rpmName;
    private String version;
    private String archName;
    private String osName;

    public DebDeployment(String outputDir) {
        this(outputDir, "ks", Deployment.defaultVersion());
    }

    public DebDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("rpmName") String rpmName,
                         @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.rpmName = rpmName;
        this.version = version;
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(PROP_OUTPUTDIR, PROP_RPMNAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_RPMNAME, rpmName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputDir = props.getOrDefault(PROP_OUTPUTDIR, outputDir);
        rpmName = props.getOrDefault(PROP_RPMNAME, rpmName);
    }

    @Override
    public DeploymentValidation validate() {
        return null;
    }

    @Override
    public String outputString() {
        File outFile = new File(outputDir, rpmName);
        StringBuilder sb = new StringBuilder();
        sb.append("DEB location:   ").append(outFile.getAbsolutePath()).append("\n");
        String size;
        if(outFile.exists()){
            long bytes = outFile.length();
            double bytesPerMB = 1024 * 1024;
            double mb = bytes / bytesPerMB;
            size = String.format("%.2f", mb) + " MB";
        } else {
            size = "<DEB not found>";
        }
        sb.append("DEB size:       ").append(size);

        return sb.toString();
    }

    @Override
    public List<String> gradleImports() {
        List<String> retVal = new ArrayList<>();
        retVal.add("org.redline_rpm.header.Os");
        retVal.add("org.redline_rpm.header.Architecture");
        return retVal;
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        return Collections.singletonList(new GradlePlugin("nebula.ospackage", "8.3.0"));
    }

    @Override
    public List<String> gradleTaskNames() {
        List<String> ret = new ArrayList<>();
        ret.add("buildDeb");
        ret.add("copyDeb");
        return ret;
    }
}
