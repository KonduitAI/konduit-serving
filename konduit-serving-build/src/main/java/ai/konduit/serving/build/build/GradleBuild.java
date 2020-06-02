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
import ai.konduit.serving.build.deployments.UberJarDeployment;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.nd4j.common.base.Preconditions;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

public class GradleBuild {

    public static void generateGradleBuildFiles(File outputDir, Config config) throws IOException {

        File gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew.bat")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        //Generate build.gradle.kts (and gradle.properties if necessary)

        StringBuilder kts = new StringBuilder();
        //kts.append("apply( plugin = 'java')").append("\n");
        kts.append("plugins {\n\"java\"\n}").append("\n");
        kts.append("\trepositories {\nmavenCentral()\n}\n");

        List<Dependency> dependencies = config.resolveDependencies();
        if (!dependencies.isEmpty()) {
            kts.append("dependencies {\n");
        }
        for (Dependency dep : dependencies) {
            /*kts.append("\tapi('" + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version() + "')").
                    append("\n");*/
            /*kts.append("\timplementation(\"" + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version() + "\")").
                    append("\n");*/
        }
        if (!dependencies.isEmpty()) {
            kts.append("}").append("\n");
        }

        List<Deployment> deployments = config.deployments();
        if (!deployments.isEmpty())
            kts.append("tasks.register<Jar>(\"uberJar\") {\n");
        for (Deployment deployment : deployments) {
            if (deployment instanceof UberJarDeployment) {
                //kts.append("\tarchiveName '(" + ((UberJarDeployment)deployment).jarName() + "')").append("\n");
                //kts.append("manifest {\nattributes 'Main-Class': '" + ((UberJarDeployment)deployment).artifactId() + "'}").append("\n");
            }
        }
        if (!deployments.isEmpty())
            kts.append("}").append("\n");
        System.out.println(kts.toString());

        Preconditions.checkState(!deployments.isEmpty(), "No deployments were specified");

        System.out.println("Dependencies: " + dependencies);
        System.out.println("Deployments: " + deployments);

        File ktsFile = new File(outputDir, "build.gradle.kts");
        FileUtils.writeStringToFile(ktsFile, kts.toString(), Charset.defaultCharset());
    }

    public static void runGradleBuild(File directory) throws IOException {
        //Check for build.gradle.kts, properties
        //Check for gradlew/gradlew.bat
        File kts = new File(directory, "build.gradle.kts");
        if (!kts.exists()) {
            throw new IllegalStateException("build.gradle.kts doesn't exist");
        }
        File gradlew = new File("target/classes/gradlew.bat");
        if (!gradlew.exists()) {
            throw new IllegalStateException("gradlew.bat doesn't exist");
        }
        //Execute gradlew
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(directory)
                .connect();

        try {
            connection.newBuild().forTasks("wrapper").run();
        } finally {
            connection.close();
        }
    }
}
