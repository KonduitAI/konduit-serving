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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface DependencyRequirement {

    String name();

    //TODO proper descriptions
    default String description(){
        return name();
    }

    boolean satisfiedBy(Target target, Collection<Dependency> currentDeps);


    public static final ModuleRequirements ND4J_BACKEND_REQ = new ModuleRequirements(Collections.unmodifiableList(Arrays.asList(
            //TODO not hardcoded version
            //Need ND4J backend (no classifier)
            new Any("nd4j backend",
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null),
                    new Dependency("org.nd4j", "nd4j-cuda-10.0", "1.0.0-beta7", null),
                    new Dependency("org.nd4j", "nd4j-cuda-10.1", "1.0.0-beta7", null),
                    new Dependency("org.nd4j", "nd4j-cuda-10.2", "1.0.0-beta7", null)),
            //ND4J backend classifiers
            new Any("nd4j backend classifier",
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64"),
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64-avx2"),
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64-avx512"),
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "windows-x86_64"),
                    new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "windows-x86_64-avx2"))
            //TODO others
    )));
}
