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

package ai.konduit.serving.build.build;

import ai.konduit.serving.build.config.Config;
import ai.konduit.serving.build.config.Deployment;
import ai.konduit.serving.build.dependencies.Dependency;
import org.nd4j.common.base.Preconditions;

import java.io.File;
import java.util.List;

public class GradleBuild {

    public static void generateGradleBuildFiles(File outputDir, Config config){

        //Add gradlew

        //Generate build.gradle.kts (and gradle.properties if necessary)

        List<Dependency> dependencies = config.resolveDependencies();
        List<Deployment> deployments = config.deployments();

        Preconditions.checkState(!deployments.isEmpty(), "No deployments were specified");

        System.out.println("Dependencies: " + dependencies);
        System.out.println("Deployments: " + deployments);


        throw new UnsupportedOperationException("Gradle generation: Not yet implemented");
    }

    public static void runGradleBuild(File directory){
        //Check for build.gradle.kts, properties
        //Check for gradlew/gradlew.bat

        //Execute gradlew


        throw new UnsupportedOperationException("Not yet implemented");
    }

}
