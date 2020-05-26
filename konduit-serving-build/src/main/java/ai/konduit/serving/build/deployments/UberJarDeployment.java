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

import ai.konduit.serving.build.config.Deployment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
public class UberJarDeployment implements Deployment {
    private String outputDir;
    private String jarName;
    private String groupId;
    private String artifactId;
    private String version;

    public UberJarDeployment(@JsonProperty("outputDir") String outputDir, @JsonProperty("jarName") String jarName,
                             @JsonProperty("groupId") String groupId, @JsonProperty("artifactId") String artifactId,
                             @JsonProperty("version") String version){
        this.outputDir = outputDir;
        this.jarName = jarName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
