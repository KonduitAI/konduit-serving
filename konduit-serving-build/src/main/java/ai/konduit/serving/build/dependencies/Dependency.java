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

package ai.konduit.serving.build.dependencies;

import ai.konduit.serving.build.dependencies.nativedep.NativeDependencyRegistry;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.common.base.Preconditions;

@Data
@Accessors(fluent = true)
public class Dependency {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;

    public Dependency(String groupId, String artifactId, String version, String classifier){
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    public boolean isNativeDependency(){
        return NativeDependencyRegistry.isNativeDependency(this);
    }

    public NativeDependency getNativeDependency(){
        Preconditions.checkState(isNativeDependency(), "Can only get NativeDependency information if the depnedency has native code");
        return NativeDependencyRegistry.getNativeDependency(this);
    }

    public String gavString(){
        return groupId + ":" + artifactId + ":" + version + (classifier == null ? "" : ":" + classifier);
    }
}
