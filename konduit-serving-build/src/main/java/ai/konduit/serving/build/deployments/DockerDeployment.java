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
import ai.konduit.serving.build.config.SimpleDeploymentValidation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.*;


@Data
@Accessors(fluent = true)
public class DockerDeployment implements Deployment {

    public static final String DEFAULT_BASE_IMAGE = "openjdk:8-jre";
    public static final String DEFAULT_IMAGE_NAME = "ks";
    public static final String PROP_BASE_IMG = "docker.baseimage";
    public static final String PROP_NAME = "docker.name";

    private String baseImage;
    private String inputDir;
    private String imageName;
    private String version;
    private String imageId;         //Should be in form: "somerepo:version"

    public DockerDeployment() {
        this(DEFAULT_BASE_IMAGE, "ks", Deployment.defaultVersion());
    }

    public DockerDeployment(@JsonProperty("baseImage") String baseImage, @JsonProperty("rpmName") String imageName,
                            @JsonProperty("version") String version){
        this.baseImage = baseImage;
        this.imageName = imageName;
        this.version = version;
    }

    @Override
    public List<String> propertyNames() {
        return Arrays.asList(PROP_BASE_IMG, PROP_NAME);
    }

    @Override
    public Map<String, String> asProperties() {
        Map<String,String> m = new LinkedHashMap<>();
        m.put(PROP_BASE_IMG, baseImage);
        m.put(PROP_NAME, imageName);
        return m;
    }

    @Override
    public void fromProperties(Map<String, String> props) {
        baseImage = props.getOrDefault(PROP_BASE_IMG, baseImage);
        imageName = props.getOrDefault(PROP_NAME, imageName);
    }

    @Override
    public DeploymentValidation validate() {
        if(baseImage == null || baseImage.isEmpty()){
            return new SimpleDeploymentValidation("No base image name is set (property: " + PROP_BASE_IMG + ")");
        }
        return new SimpleDeploymentValidation();
    }

    @Override
    public String outputString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JAR location:        ");
        sb.append("Docker image name:   ").append(imageName).append("\n");
        sb.append("Docker base image:   ").append(baseImage).append("\n");
        sb.append("Docker image id:     ").append(imageId).append("\n");
        return sb.toString();
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
    public List<String> gradleTaskNames() {
        return Collections.singletonList("buildImage");
    }
}
