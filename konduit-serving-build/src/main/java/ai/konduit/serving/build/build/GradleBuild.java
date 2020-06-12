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
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.deployments.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.nd4j.common.base.Preconditions;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GradleBuild {

    private static String createCopyTask(String taskName, String fromDir, String toDir, String fileMask,
                                         String pluginOutput) {
        String built = (fromDir + File.separator + "build" + File.separator + pluginOutput).
                replace("\\","\\\\");;
        String deployed = (toDir.replace("\\","\\\\"));

        String retVal =  "tasks.register<Copy>(\"" + taskName + "\") {\n" +
                         "\t from(\""  + built + "\")\n" +
                         "\t include(\"" + fileMask + "\")\n" +
                         "\t into(\""  + deployed + "\")\n}\n";
        return retVal;
    }

    public static void generateGradleBuildFiles(File outputDir, Config config) throws IOException {

        //TODO We need a proper solution for this!
        //For now - the problem with the creation of a manifest (only) JAR is that the "tasks.withType(Jar::class)" gets
        // put into the uber-jar.
        boolean uberjar = false;
        boolean classpathMF = false;
        for(Deployment d : config.deployments()){
            uberjar |= d instanceof UberJarDeployment;
            classpathMF = (d instanceof ClassPathDeployment && ((ClassPathDeployment) d).type() == ClassPathDeployment.Type.JAR_MANIFEST);
        }
        Preconditions.checkState(uberjar != classpathMF || !uberjar, "Unable to create both a classpath manifest (ClassPathDeployment)" +
                " and uber-JAR deployment at once");

        File gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        gradlewResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("gradlew.bat")));
        if (gradlewResource.exists())
            FileUtils.copyFileToDirectory(gradlewResource, outputDir);

        File dockerResource = new File(String.valueOf(GradleBuild.class.getClassLoader().getResource("Dockerfile")));
        if (dockerResource.exists())
            FileUtils.copyFileToDirectory(dockerResource, outputDir);

        //Generate build.gradle.kts (and gradle.properties if necessary)
        StringBuilder kts = new StringBuilder();

        for(Deployment d : config.deployments()){
            List<String> imports = d.gradleImports();
            if(imports != null && !imports.isEmpty()){
                for(String s : imports) {
                    kts.append("import ").append(s).append("\n");
                }
            }
        }

        kts.append("plugins { java \n");
        for(Deployment d : config.deployments()){
            List<GradlePlugin> gi = d.gradlePlugins();
            if(gi != null && !gi.isEmpty()){
                for(GradlePlugin g : gi) {
                    if (StringUtils.isNotEmpty(g.version()))
                        kts.append("id(\"").append(g.id()).append("\"").append(") version \"").append(g.version()).append("\"\n");
                    else
                        kts.append("id(\"").append(g.id()).append("\")\n");
                }
            }
        }
        kts.append("\n}")
                .append("\n");

        kts.append("repositories {\n" +
                "\tmavenCentral()\n" +
                "\tmavenLocal()\n" +
                "\tjcenter()\n" +
                "\tmaven {\n\turl = uri(\"https://plugins.gradle.org/m2/\")}\n}\n");
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
            else
                kts.append("\timplementation(\"" + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version() + ":" + dep.classifier() + "\")").
                        append("\n");
        }
        if (!dependencies.isEmpty()) {
            kts.append("}").append("\n");
        }

        List<Deployment> deployments = config.deployments();
        Preconditions.checkState(deployments != null, "No deployments (uberjar, docker, etc) were specified for the build");

        for (Deployment deployment : deployments) {
            if (deployment instanceof UberJarDeployment) {
                kts.append("tasks.withType<ShadowJar> {\n");
                String jarName = ((UberJarDeployment)deployment).jarName();
                if(jarName.endsWith(".jar")){
                    jarName = jarName.substring(0, jarName.length()-4);
                }

                kts.append("\tbaseName = \"" + jarName + "\"\n");
                String escaped = ((UberJarDeployment)deployment).outputDir().replace("\\","\\\\");
                kts.append("\tdestinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("\tmergeServiceFiles()");  //For service loader files
                kts.append("}\n");
            }
            else if (deployment instanceof RpmDeployment) {
                String rpmName = ((RpmDeployment)deployment).rpmName();
                kts.append("ospackage { \n");
                if(rpmName.endsWith(".rpm")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }

                kts.append("\tpackageName = \"" + rpmName + "\"\n");
                kts.append("\tsetArch( " + ((RpmDeployment)deployment).archName() + ")\n");
                kts.append("\tos = " + ((RpmDeployment)deployment).osName() + "\n");
                kts.append("}\n");

                kts.append(createCopyTask("copyRpm", outputDir.getAbsolutePath(),
                        ((RpmDeployment)deployment).outputDir(), "*.rpm", "distributions"));
            }
            else if (deployment instanceof DebDeployment) {
                String rpmName = ((DebDeployment)deployment).rpmName();
                kts.append("ospackage {\n");
                if(rpmName.endsWith(".deb")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }

                kts.append("\tpackageName = \"" + rpmName + "\"\n");
                //kts.append("\tsetArch(" + ((DebDeployment)deployment).archName() + ")\n");
                kts.append("\tos = " + ((DebDeployment)deployment).osName() + "\n");
                kts.append("}").append("\n\n");

                kts.append(createCopyTask("copyDeb", outputDir.getAbsolutePath(), ((DebDeployment)deployment).outputDir(),
                        "*.deb", "distributions"));
            } else if(deployment instanceof ClassPathDeployment){
                addClassPathTask(kts, (ClassPathDeployment) deployment);
            }
            else if (deployment instanceof ExeDeployment) {
                String exeName = ((ExeDeployment)deployment).exeName();
                kts.append("tasks.withType<DefaultLaunch4jTask> {\n");
                if(exeName.endsWith(".exe")){
                    exeName = exeName.substring(0, exeName.length()-4);
                }
                kts.append("\toutfile = \"" + exeName + ".exe\"\n");
                //kts.append("destinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("\tmainClassName = \"ai.konduit.serving.launcher.KonduitServingLauncher\"\n");
                kts.append("}\n");
                kts.append(createCopyTask("copyExe", outputDir.getAbsolutePath(), ((ExeDeployment)deployment).outputDir(),
                        "*.exe", "launch4j"));
            }

            else if (deployment instanceof DockerDeployment) {
                String escaped = dockerResource.getParent().replace("\\","\\\\");
                kts.append("tasks.create(\"buildImage\", DockerBuildImage::class) {\n" +
                        "\tinputDir.set(file(\"" + escaped + "\"))\n" +
                    "}\n");
            }
            else if (deployment instanceof TarDeployment) {
                List<String> fromFiles = ((TarDeployment)deployment).files();
                if (fromFiles.size() > 0) {
                    String rpmName = ((TarDeployment) deployment).archiveName();
                    kts.append("distributions {\n");
                    kts.append("\tmain {\n");

                    kts.append("\t\tdistributionBaseName.set( \"" + rpmName + "\")\n");
                    kts.append("\t\t contents {\n");
                    for (String file : fromFiles) {
                        String escaped = file.replace("\\","\\\\");
                        kts.append("\t\t\tfrom(\"" + escaped + "\")\n");
                    }
                    kts.append("\t\t }\n");
                    kts.append("\t}\n");
                    kts.append("}").append("\n\n");

                    kts.append(createCopyTask("copyTar", outputDir.getAbsolutePath(), ((TarDeployment)deployment).outputDir(),
                            "*.tar", "distributions"));
                }
            }
        }

        System.out.println(kts.toString());

        Preconditions.checkState(!deployments.isEmpty(), "No deployments were specified");

        System.out.println("Dependencies: " + dependencies);
        System.out.println("Deployments: " + deployments);



        File ktsFile = new File(outputDir, "build.gradle.kts");
        FileUtils.writeStringToFile(ktsFile, kts.toString(), Charset.defaultCharset());
    }

    public static void runGradleBuild(File directory, Config config) throws IOException {
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
        List<String> tasks = new ArrayList<>();
        tasks.add("wrapper");
        for(Deployment d : config.deployments()){
            for (String s : d.gradleTaskNames()) {
                if (!tasks.contains(s)) {
                    tasks.add(s);
                }
            }
        }

        try {
            connection.newBuild().setStandardOutput(System.out).setStandardError(System.err).forTasks(tasks.toArray(new String[0])).run();
        } finally {
            connection.close();
        }
    }

    public static String getRpmDebArch(Target t){
        //https://github.com/craigwblake/redline/blob/master/src/main/java/org/redline_rpm/header/Architecture.java
        switch (t.arch()){
            case x86:
            case x86_avx2:
            case x86_avx512:
                return "X86_64";
            case armhf:
                return "ARM";
            case arm64:
                return "AARCH64";
            case ppc64le:
                return "PPC64";
            default:
                throw new RuntimeException("Unknown arch for target: " + t);
        }
    }

    private static void addClassPathTask(StringBuilder kts, ClassPathDeployment cpd){
        //Adapted from: https://stackoverflow.com/a/54159784
        if(cpd.type() == ClassPathDeployment.Type.TEXT_FILE) {
            kts.append("//Task: ClassPathDeployment - writes the absolute path of all JAR files for the build to the specified text file, one per line\n")
                    .append("task(\"writeClassPathToFile\"){\n")
                    .append("    var spec2File: Map<String, File> = emptyMap()\n")
                    .append("    configurations.compileClasspath {\n")
                    .append("        val s2f: MutableMap<ResolvedModuleVersion, File> = mutableMapOf()\n")
                    .append("        // https://discuss.gradle.org/t/map-dependency-instances-to-file-s-when-iterating-through-a-configuration/7158\n")
                    .append("        resolvedConfiguration.resolvedArtifacts.forEach({ ra: ResolvedArtifact ->\n")
                    .append("            s2f.put(ra.moduleVersion, ra.file)\n").append("        })\n")
                    .append("        spec2File = s2f.mapKeys({\"${it.key.id.group}:${it.key.id.name}\"})\n")
                    .append("        spec2File.keys.sorted().forEach({ it -> println(it.toString() + \" -> \" + spec2File.get(it))})\n")
                    .append("        val sb = StringBuilder()\n")
                    .append("        spec2File.keys.sorted().forEach({ it -> sb.append(spec2File.get(it)); sb.append(\"\\n\")})\n")
                    .append("        File(\"").append(cpd.outputFile()).append("\").writeText(sb.toString())\n")
                    .append("    }\n")
                    .append("}\n");
        } else {
            //Write a manifest JAR
            kts.append("//Write a JAR with a manifest containin the path of all dependencies, but no other content\n")
                    .append("tasks.withType(Jar::class) {\n")
                    .append("    manifest {\n")
                    .append("        attributes[\"Manifest-Version\"] = \"1.0\"\n")
                    .append("        attributes[\"Class-Path\"] = configurations.runtimeClasspath.get().getFiles().joinToString(separator=\" \")\n")
                    .append("    }\n");

            if(cpd.outputFile() != null){
                String path = cpd.outputFile().replace("\\", "/");
                kts.append("setProperty(\"archiveFileName\", \"").append(path).append("\")\n");
            }

            kts.append("}");
        }
    }
}
