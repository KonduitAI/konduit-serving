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
import ai.konduit.serving.build.deployments.DebDeployment;
import ai.konduit.serving.build.deployments.ExeDeployment;
import ai.konduit.serving.build.deployments.RpmDeployment;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.nd4j.common.base.Preconditions;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class GradleBuild {

    private static List<String> csvTasks = new ArrayList<>();

    public static void generateGradleBuildFiles(File outputDir, Config config) throws IOException {

        csvTasks.clear();
        csvTasks.add("wrapper");

        File gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew.bat")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        //Generate build.gradle.kts (and gradle.properties if necessary)
        StringBuilder kts = new StringBuilder();
        kts.append("import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar\n");
        kts.append("import edu.sc.seis.launch4j.tasks.DefaultLaunch4jTask\n");
        kts.append("import org.redline_rpm.header.Os\n");
        kts.append("plugins { id(\"java\") \n id(\"com.github.johnrengelman.shadow\") version \"2.0.4\"\n" +
                "id(\"nebula.rpm\") version \"8.3.0\" \n" +
                "id(\"nebula.deb\") version \"8.3.0\" \n" +
                "id(\"nebula.ospackage\") version \"8.3.0\" \n" +
                "id(\"edu.sc.seis.launch4j\") version \"2.4.6\"}\n");

        /*kts.append("plugins { id(\"java\") \n id(\"com.github.johnrengelman.shadow\") \n" +
                "id(\"nebula.rpm\") \n" +
                "id(\"nebula.deb\") \n" +
                "id(\"edu.sc.seis.launch4j\") }\n");*/


        kts.append("\trepositories {\nmavenCentral()\nmavenLocal()\njcenter()\n" +
                "maven {\nurl = uri(\"https://plugins.gradle.org/m2/\")}\n}\n");
        kts.append("group = \"ai.konduit\"\n");
        //kts.append("version = \"1.0-SNAPSHOT\"\n");

        List<Dependency> dependencies = config.resolveDependencies();
        if (!dependencies.isEmpty()) {
            kts.append("dependencies {\n");
        }
        for (Dependency dep : dependencies) {
            if (dep.classifier() == null)
                kts.append("\timplementation(\"" + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version() + "\")").
                        append("\n");
            /*else
                kts.append("\timplementation(\"" + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version() + ":" + dep.classifier() + "\")").
                        append("\n");*/
        }
        if (!dependencies.isEmpty()) {
            kts.append("}").append("\n");
        }


        List<Deployment> deployments = config.deployments();
        Preconditions.checkState(deployments != null, "No deployments (uberjar, docker, etc) were specified for the build");

        for (Deployment deployment : deployments) {
            if (deployment instanceof UberJarDeployment) {
                kts.append("\ttasks.withType<ShadowJar> {\n");
                String jarName = ((UberJarDeployment)deployment).jarName();
                if(jarName.endsWith(".jar")){
                    jarName = jarName.substring(0, jarName.length()-4);
                }

                kts.append("\tbaseName = \"" + jarName + "\"\n");
                String escaped = ((UberJarDeployment)deployment).outputDir().replace("\\","\\\\");
                kts.append("destinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("mergeServiceFiles()");  //For service loader files
                kts.append("}\n");

                csvTasks.add("shadowJar");
            }
            else if (deployment instanceof RpmDeployment) {
                String rpmName = ((RpmDeployment)deployment).rpmName();
                kts.append("ospackage { \n");
                if(rpmName.endsWith(".rpm")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }

                kts.append("\tpackageName = \"" + rpmName + "\"\n");
                //kts.append("\tarch = \"" + ((RpmDeployment)deployment).archName() + "\"\n");
                //kts.append("\tos = \"" + ((RpmDeployment)deployment).osName() + "\"\n");
                kts.append("\tos = Os.LINUX\n");
                // String escaped = ((RpmDeployment)deployment).outputDir().replace("\\","\\\\");
                //kts.append("\tdestinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("}\n");

                csvTasks.add("buildRpm");
            }
            else if (deployment instanceof DebDeployment) {
                String rpmName = ((DebDeployment)deployment).rpmName();
                kts.append("ospackage {\n");
                if(rpmName.endsWith(".deb")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }

                kts.append("packageName = \"" + rpmName + "\"\n");
                /*kts.append("arch = \"" + ((DebDeployment)deployment).archName() + "\"\n");
                kts.append("os = \"" + ((DebDeployment)deployment).osName() + "\"\n");
                String escaped = ((DebDeployment)deployment).outputDir().replace("\\","\\\\");
                kts.append("destinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("mergeServiceFiles()\n");  //For service loader files*/
                kts.append("}").append("\n");

                csvTasks.add("buildDeb");
            }
            else if (deployment instanceof ExeDeployment) {
                String exeName = ((ExeDeployment)deployment).exeName();
                kts.append("tasks.withType<DefaultLaunch4jTask> {\n");
                if(exeName.endsWith(".exe")){
                    exeName = exeName.substring(0, exeName.length()-4);
                }

                kts.append("outfile = \"" + exeName + ".exe\"\n");
                String escaped = ((ExeDeployment)deployment).outputDir().replace("\\","\\\\");
                //kts.append("outputDir = \"" + escaped + "\")\n");
                //kts.append("destinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("mainClassName = \"ai.konduit.serving.launcher.KonduitServingLauncher\"\n");
                //kts.append("mergeServiceFiles()\n");  //For service loader files
                kts.append("}\n");

                csvTasks.add("createExe");
            }
        }

        //System.out.println(kts.toString());

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
                //.useGradleVersion("6.1")
                .connect();

        try {
            String[] tasksList = new String[csvTasks.size()];
            csvTasks.toArray(tasksList);
            connection.newBuild().forTasks(tasksList).run();
            } finally {
            connection.close();
        }
    }
}
