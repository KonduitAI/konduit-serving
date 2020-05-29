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

import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.nativedep.NativeDependency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class AllRequirement implements DependencyRequirement {
    private final String name;
    private Set<Dependency> set;

    public AllRequirement(String name, Dependency... dependencies){
        this.name = name;
        this.set = new HashSet<>(Arrays.asList(dependencies));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean satisfiedBy(Target target, Collection<Dependency> currDeps) {
        //We need ALL of the requirements to be satisfied (considering native code + target)
        for (Dependency need : set) {
            boolean matchFound = false;
            for (Dependency d : currDeps) {
                if (need.equals(d)) {
                    //GAV(C) match, but maybe it's a native dependency, and platform doesn't match
                    if (need.isNativeDependency()) {
                        NativeDependency nd = need.getNativeDependency();
                        if (nd.supports(target)) {
                            matchFound = true;
                            break;
                        }
                    } else {
                        //Pure Java dependency
                        matchFound = true;
                        break;
                    }
                }
            }
            if(!matchFound)
                return false;
        }
        return true;
    }

    @Override
    public List<DependencyAddition> suggestDependencies(Target target, Collection<Dependency> currDeps) {
        if(satisfiedBy(target, currDeps))
            return null;

        //We need ALL of the requirements to be satisfied (considering native code + target)
        Set<Dependency> notFound = new HashSet<>();
        for (Dependency need : set) {
            boolean matchFound = false;
            for (Dependency d : currDeps) {
                if (need.equals(d)) {
                    //GAV(C) match, but maybe it's a native dependency, and platform doesn't match
                    if (need.isNativeDependency()) {
                        NativeDependency nd = need.getNativeDependency();
                        if (nd.supports(target)) {
                            matchFound = true;
                            break;
                        }
                    } else {
                        //Pure Java dependency
                        matchFound = true;
                        break;
                    }
                }
            }

            if(!matchFound){
                notFound.add(need);
            }
        }

        if(notFound.isEmpty())
            return null;

        return Collections.singletonList(new AllAddition(new ArrayList<>(notFound), this));
    }
}
