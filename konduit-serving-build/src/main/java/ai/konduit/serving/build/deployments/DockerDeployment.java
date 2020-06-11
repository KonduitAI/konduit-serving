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
import lombok.Setter;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.*;

public class DockerDeployment implements Deployment {

    public static final String DEFAULT_IMAGE_NAME = "ks";
    public static final String PROP_OUTPUTDIR = "docker.outputdir";
    public static final String PROP_RPMNAME = "docker.name";

    private String outputDir;
    @Setter
    private String imageName;
    private String version;

    public DockerDeployment(String outputDir) {
        this(outputDir, "ks", Deployment.defaultVersion());
    }

    public DockerDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("rpmName") String imageName,
                             @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.imageName = imageName;
        this.version = version;
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(DEFAULT_IMAGE_NAME, PROP_OUTPUTDIR, PROP_RPMNAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_OUTPUTDIR, outputDir);
        m.put(PROP_RPMNAME, imageName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        outputDir = props.getOrDefault(PROP_OUTPUTDIR, outputDir);
        imageName = props.getOrDefault(PROP_RPMNAME, imageName);
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
        return Collections.singletonList("com.bmuschko.gradle.docker.tasks.image.*");
    }

    @Override
    public List<GradlePlugin> gradlePlugins() {
        return Collections.singletonList(new GradlePlugin("com.bmuschko.docker-remote-api", "6.4.0"));
    }

    @Override
    public String gradleTaskName() {
        return "buildImage";
    }
}
