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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

@Data
@AllArgsConstructor
@Accessors(fluent = true)
public class ModuleRequirements {

    private List<DependencyRequirement> reqs;

    public boolean satisfiedBy(Target target, Collection<Dependency> currentDeps){
        for(DependencyRequirement req : reqs){
            if(!req.satisfiedBy(target, currentDeps))
                return false;
        }
        return true;
    }

    public List<DependencyAddition> suggestDependencies(Target target, Collection<Dependency> currentDeps){
        if(satisfiedBy(target, currentDeps))
            return null;

        List<DependencyAddition> l = new ArrayList<>();
        for(DependencyRequirement r : reqs ){
            if(r.satisfiedBy(target, currentDeps))
                continue;

            //This requirement is not satisfied...
            l.addAll(r.suggestDependencies(target, currentDeps));
        }

        //TODO we should filter for duplicates

        return l;
    }

}
