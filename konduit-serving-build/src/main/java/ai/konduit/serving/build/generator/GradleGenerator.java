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

package ai.konduit.serving.build.generator;

import ai.konduit.serving.build.config.Config;

import java.io.File;

public class GradleGenerator {

    public void generateGradle(File outputDir, Config config){

        //Add gradlew

        //Generate build.gradle.kts (and gradle.properties if necessary)

        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void runGradleBuild(File directory){

        throw new UnsupportedOperationException("Not yet implemented");
    }

}
