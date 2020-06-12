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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.*;


@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class TarDeployment implements Deployment {

    public static final String DEFAULT_ARCHIVE_NAME = "ks";
    public static final String PROP_OUTPUTDIR = "tar.outputdir";
    public static final String PROP_ARCHIVENAME = "tar.name";

    @Getter
    private String outputDir;
    @Setter
    @Getter
    private String archiveName;
    private String version;
    @Setter
    @Getter
    private List<String> files;

    public TarDeployment(String outputDir) {
        this(outputDir, "ks", Deployment.defaultVersion());
    }

    public TarDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("rpmName") String imageName,
                            @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.archiveName = imageName;
        this.version = version;
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(DEFAULT_ARCHIVE_NAME, PROP_OUTPUTDIR, PROP_ARCHIVENAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_ARCHIVENAME, archiveName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputDir = props.getOrDefault(PROP_OUTPUTDIR, outputDir);
        archiveName = props.getOrDefault(PROP_ARCHIVENAME, archiveName);
    }

    @Override
    public DeploymentValidation validate() {
        return null;
    }

    @Override
    public String outputString() {
        return null;
    }

    @Override
    public List<String> gradleImports() {
        return null;
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        return Collections.singletonList(new GradlePlugin("distribution", ""));
    }

    @Override
    public List<String> gradleTaskNames() {
        List<String> ret = new ArrayList<>();
        ret.add("distTar");
        ret.add("copyTar");
        return ret;
    }
}
