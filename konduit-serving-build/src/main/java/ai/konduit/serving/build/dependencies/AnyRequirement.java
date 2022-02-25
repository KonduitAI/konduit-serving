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

package ai.konduit.serving.build.dependencies;

import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.nativedep.NativeDependency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class AnyRequirement implements DependencyRequirement {
    private final String name;
    private final Set<Dependency> set;

    public AnyRequirement(String name, List<Dependency> dependencies) {
        this(name, new HashSet<>(dependencies));
    }

    public AnyRequirement(String name, Dependency... dependencies) {
        this.name = name;
        set = new HashSet<>(Arrays.asList(dependencies));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean satisfiedBy(Target target, Collection<Dependency> currDeps) {
        //Only need one of the requirements to be satisfied (considering native code + target)
        for (Dependency need : set) {
            for (Dependency d : currDeps) {
                if (need.equals(d)) {
                    //GAV(C) match, but maybe it's a native dependency, and platform doesn't match
                    if (need.isNativeDependency()) {
                        NativeDependency nd = need.getNativeDependency();
                        if (nd.supports(target)) {
                            return true;
                        }
                    } else {
                        //Pure Java dependency
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<DependencyAddition> suggestDependencies(Target target, Collection<Dependency> currentDeps) {
        if(satisfiedBy(target, currentDeps))
            return null;

        //If not already satisfied, it means that none of the dependencies are available
        //But we still have to filter by what can run on this target
        List<Dependency> out = new ArrayList<>();
        for(Dependency d : set){
            if(d.isNativeDependency()){
                NativeDependency nd = d.getNativeDependency();
                if(nd.supports(target)){
                    out.add(d);
                }
            } else {
                out.add(d);
            }
        }

        if(out.isEmpty())
            return null;

        return Collections.singletonList(new AnyAddition(out, this));
    }
}
