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

@Data
@Accessors(fluent = true)
public class Module {
    //TODO these requirements should probably be defined somewhere else!
    public static final Module PIPELINE = new Module("konduit-serving-pipeline", ksModule("konduit-serving-pipeline"), null, null);
    public static final Module DL4J = new Module("konduit-serving-deeplearning4j", ksModule("konduit-serving-deeplearning4j"), DependencyRequirement.ND4J_BACKEND_REQ, null);
    public static final Module SAMEDIFF = new Module("konduit-serving-samediff", ksModule("konduit-serving-samediff"), DependencyRequirement.ND4J_BACKEND_REQ, null);
//    public static final Module TENSORFLOW = new Module("konduit-serving-tensorflow");

    private final String name;
    private final Dependency dependency;
    private final ModuleRequirements dependencyRequirements;
    private final ModuleRequirements dependencyOptional;

    public Module(@JsonProperty("name") String name, @JsonProperty("dependency") Dependency dependency,
                  @JsonProperty("dependencyRequirements") ModuleRequirements dependencyRequirements,
                  @JsonProperty("dependencyOptional") ModuleRequirements dependencyOptional){
        this.name = name;
        this.dependency = dependency;
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

    protected static Dependency ksModule(String name){
        //TODO don't hardcode versions
        return new Dependency("ai.konduit.serving", name, "0.1.0-SNAPSHOT", null);
    }
}
