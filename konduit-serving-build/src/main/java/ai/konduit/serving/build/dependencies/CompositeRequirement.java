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

import ai.konduit.serving.build.config.target.Target;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class CompositeRequirement implements DependencyRequirement {
    public enum Type {ANY, ALL}

    private final Type type;
    private DependencyRequirement[] reqs;

    public CompositeRequirement(Type type, List<DependencyRequirement> reqs){
        this(type, reqs.toArray(new DependencyRequirement[0]));
    }

    public CompositeRequirement(Type type, DependencyRequirement... reqs){
        this.type = type;
        this.reqs = reqs;
    }


    @Override
    public String name() {
        return "";  //TODO
    }

    @Override
    public boolean satisfiedBy(Target target, Collection<Dependency> currentDeps) {
        boolean anySatisfied = false;
        boolean allSatisfied = true;
        for(DependencyRequirement r : reqs){
            boolean thisSat = r.satisfiedBy(target, currentDeps);
            anySatisfied |= thisSat;
            allSatisfied &= thisSat;
        }
        if(type == Type.ANY){
            return anySatisfied;
        } else {
            return allSatisfied;
        }
    }

    @Override
    public List<DependencyAddition> suggestDependencies(Target target, Collection<Dependency> currentDeps) {
        //TODO this should be reconsidered - what if multiple sub-requirements make the same recommendation?
        List<DependencyAddition> l = new ArrayList<>();
        for(DependencyRequirement r : reqs){
            List<DependencyAddition> add = r.suggestDependencies(target, currentDeps);
            if(add != null) {
                l.addAll(add);
            }
        }
        return l;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(type.toString()).append("(");
        List<String> l = new ArrayList<>();
        for(DependencyRequirement d : reqs)
            l.add(d.toString());
        sb.append(String.join(",", l));
        sb.append(")");
        return sb.toString();
    }
}
