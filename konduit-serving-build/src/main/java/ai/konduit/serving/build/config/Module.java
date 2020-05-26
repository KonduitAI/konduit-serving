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

package ai.konduit.serving.build.config;

import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.dependencies.DependencyRequirement;
import ai.konduit.serving.build.dependencies.ModuleRequirements;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(fluent = true)
public class Module {
    //TODO these requirements should probably be defined somewhere else!
    public static final Module PIPELINE = new Module("konduit-serving-pipeline", null, null);
    public static final Module DL4J = new Module("konduit-serving-deeplearning4j", DependencyRequirement.ND4J_BACKEND_REQ, null);
//    public static final Module SAMEDIFF = new Module("konduit-serving-samediff");
//    public static final Module TENSORFLOW = new Module("konduit-serving-tensorflow");

    //TODO we should do this
    public Module forName(String name){
        String origName = name;
        if(!name.startsWith("konduit-serving-"))
            name = "konduit-serving-" + name;

        switch (name){
            case "konduit-serving-pipeline":
                return PIPELINE;
            case "konduit-serving-deeplearning4j":
                return DL4J;
            default:
                throw new RuntimeException();
        }
    }

    private final String name;
    private final ModuleRequirements dependencyRequirements;
    private final ModuleRequirements dependencyOptional;

    public Module(@JsonProperty("name") String name, @JsonProperty("dependencyRequirements") ModuleRequirements dependencyRequirements,
                  @JsonProperty("dependencyOptional") ModuleRequirements dependencyOptional){
        this.name = name;
        this.dependencyRequirements = dependencyRequirements;
        this.dependencyOptional = dependencyOptional;
    }

    public Dependency asDependency(){
        throw new UnsupportedOperationException();
    }

    public ModuleRequirements dependenciesRequired(){
        return dependencyRequirements;
    }

    public Object dependenciesOptional(){
        return dependencyOptional;
    }
}
