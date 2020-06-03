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

package ai.konduit.serving.build;

import ai.konduit.serving.build.config.Module;
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestRequirements {

    @Test
    public void testRequirementsSimple() {

        //Check that DL4J module has ND4J backend...
        Module dl4j = Module.DL4J;

        ModuleRequirements req = dl4j.dependencyRequirements();

        List<Dependency> someDep = Arrays.asList(new Dependency("org.slf4j", "slf4j-api", "1.7.26", null));
        List<Dependency> nd4jNoClassifier = Arrays.asList(someDep.get(0),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null));
        List<Dependency> withNd4j = Arrays.asList(someDep.get(0),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64")
        );


        assertFalse(req.satisfiedBy(Target.LINUX_X86, someDep));                //no nd4j-backend
        assertFalse(req.satisfiedBy(Target.LINUX_X86, nd4jNoClassifier));       //nd4j-backend, but no classifier for linux x86
        assertTrue(req.satisfiedBy(Target.LINUX_X86, withNd4j));                //Both backend and classifier for linux x86
        assertTrue(req.satisfiedBy(Target.LINUX_X86_AVX512, withNd4j));         //Should olso run on avx512 system (even if not optimal)
        assertFalse(req.satisfiedBy(Target.WINDOWS_X86, withNd4j));             //No windows dep currently
    }

    @Test
    public void testRecommendations(){
        Module dl4j = Module.DL4J;

        ModuleRequirements req = dl4j.dependencyRequirements();
        List<Dependency> someDep = Arrays.asList(new Dependency("org.slf4j", "slf4j-api", "1.7.26", null));
        List<Dependency> nd4jNoClassifier = Arrays.asList(someDep.get(0),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null));
        List<Dependency> withNd4j = Arrays.asList(someDep.get(0),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null),
                new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64")
        );

        List<DependencyAddition> l1 = req.suggestDependencies(Target.LINUX_X86, someDep);
        List<DependencyAddition> l2 = req.suggestDependencies(Target.LINUX_X86, nd4jNoClassifier);
        List<DependencyAddition> l3 = req.suggestDependencies(Target.LINUX_X86, withNd4j);

        assertEquals(2, l1.size()); //Should be nd4j-native, and nd4j-native:linux-x86_64
        assertEquals(1, l2.size()); //Should just be classifier
        assertNull(l3);     //No additions required

        List<DependencyRequirement> reqs = req.reqs();
        List<DependencyAddition> l1Exp = Arrays.asList(
                new AnyAddition(Collections.singletonList(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", null)), reqs.get(0)),
                new AnyAddition(Collections.singletonList(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64")), reqs.get(1)));
        assertEquals(l1Exp, l1);

        List<DependencyAddition> l2Exp = Collections.singletonList(new AnyAddition(Collections.singletonList(new Dependency("org.nd4j", "nd4j-native", "1.0.0-beta7", "linux-x86_64")), reqs.get(1)));
        assertEquals(l2Exp, l2);
    }
}
