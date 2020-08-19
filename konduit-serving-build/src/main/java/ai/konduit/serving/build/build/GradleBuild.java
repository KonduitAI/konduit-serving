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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.nd4j.common.base.Preconditions;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        copyResource("/gradle/gradlew", new File(outputDir, "gradlew"));
        copyResource("/gradle/gradlew.bat", new File(outputDir, "gradlew.bat"));

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

        // ----- Repositories Section -----
        kts.append("\trepositories {\nmavenCentral()\nmavenLocal()\njcenter()\n}\n");


        // ----- Plugins Section -----
        kts.append("plugins { java \n");
        /*
        //Not yet released - uncomment this once gradle-javacpp-platform plugin is available
        //Set JavaCPP platforms - https://github.com/bytedeco/gradle-javacpp#the-platform-plugin
        kts.append("id(\"org.bytedeco.gradle-javacpp-platform\") version \"1.5.3\"\n");      //TODO THIS VERSION SHOULDN'T BE HARDCODED
         */
        for(Deployment d : config.deployments()){
            List<GradlePlugin> gi = d.gradlePlugins();
            if(gi != null && !gi.isEmpty()){
                for(GradlePlugin g : gi) {
                    if (StringUtils.isNotEmpty(g.version()))
                        kts.append("\t").append("id(\"").append(g.id()).append("\"").append(") version \"").append(g.version()).append("\"\n");
                    else
                        kts.append("\t").append("id(\"").append(g.id()).append("\")\n");
                }
            }
        }
        kts.append("\n}")
            .append("\n");



        /*
        //Uncomment once gradle-javacpp-platform plugin available
        kts.append("ext {\n")
                .append("\tjavacppPlatorm = \"").append(config.target().toJavacppPlatform() + "\"\n")
                .append("}\n\n");
         */

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
                String escaped = ((UberJarDeployment)deployment).outputDir().replace("\\","\\\\");
                String jarName = ((UberJarDeployment)deployment).jarName();
                if(jarName.endsWith(".jar")){
                    jarName = jarName.substring(0, jarName.length()-4);
                }
                addUberJarTask(kts, jarName, escaped, config);
            }
            else if (deployment instanceof RpmDeployment) {
                RpmDeployment r = (RpmDeployment)deployment;
                String escaped = r.outputDir().replace("\\","\\\\");
                addUberJarTask(kts,  "ks", escaped, config);

                String rpmName = r.rpmName();
                kts.append("ospackage { \n");
                if(rpmName.endsWith(".rpm")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }
                kts.append("\tfrom(\"" + escaped + "\")\n");
                kts.append("\tpackageName = \"" + rpmName + "\"\n");
                kts.append("\tsetArch( " + getRpmDebArch(config.target()) + ")\n");
                kts.append("\tos = " + getRpmDebOs(config.target()) + "\n");
                kts.append("}\n");

                kts.append(createCopyTask("copyRpm", outputDir.getAbsolutePath(),
                        r.outputDir(), "*.rpm", "distributions"));
            }
            else if (deployment instanceof DebDeployment) {
                String escaped = ((DebDeployment)deployment).outputDir().replace("\\","\\\\");
                addUberJarTask(kts,  "ks", escaped, config);

                String rpmName = ((DebDeployment)deployment).rpmName();
                kts.append("ospackage {\n");
                if(rpmName.endsWith(".deb")){
                    rpmName = rpmName.substring(0, rpmName.length()-4);
                }
                kts.append("\tfrom(\"" + escaped + "\")\n");
                kts.append("\tpackageName = \"" + rpmName + "\"\n");
                //kts.append("\tsetArch(" + ((DebDeployment)deployment).archName() + ")\n");
                kts.append("\tos = " + getRpmDebOs(config.target()) + "\n");
                kts.append("}").append("\n\n");

                kts.append(createCopyTask("copyDeb", outputDir.getAbsolutePath(), ((DebDeployment)deployment).outputDir(),
                        "*.deb", "distributions"));
            } else if(deployment instanceof ClassPathDeployment){
                addClassPathTask(kts, (ClassPathDeployment) deployment, config);
            }
            else if (deployment instanceof ExeDeployment) {
                String exeName = ((ExeDeployment)deployment).exeName();
                kts.append("tasks.withType<DefaultLaunch4jTask> {\n");
                if(exeName.endsWith(".exe")){
                    exeName = exeName.substring(0, exeName.length()-4);
                }
                kts.append("\toutfile = \"" + exeName + ".exe\"\n");
                //kts.append("destinationDirectory.set(file(\"" + escaped + "\"))\n");
                kts.append("\tmainClassName = \"ai.konduit.serving.cli.launcher.KonduitServingLauncher\"\n");
                kts.append("}\n");
                kts.append(createCopyTask("copyExe", outputDir.getAbsolutePath(), ((ExeDeployment)deployment).outputDir(),
                        "*.exe", "launch4j"));
            }

            else if (deployment instanceof DockerDeployment) {
                String escapedInputDir = StringUtils.EMPTY;
                DockerDeployment dd = (DockerDeployment)deployment;
                if (StringUtils.isEmpty(dd.inputDir())) {
                    if (dockerResource != null)
                        escapedInputDir = dockerResource.getParent().replace("\\","\\\\");
                }
                else {
                   escapedInputDir = dd.inputDir().replace("\\", "\\\\");
                }

                kts.append("tasks.create(\"buildImage\", DockerBuildImage::class) {\n");
                if (StringUtils.isNotEmpty(escapedInputDir))
                    kts.append("\tinputDir.set(file(\"" + escapedInputDir + "\"))\n");
                else
                    kts.append("\tval baseImage = FromInstruction(From(\"").append(dd.baseImage()).append("\n");
                if(dd.imageName() != null){
                    //Note image names must be lower case
                    kts.append("\timages.add(\"").append(dd.imageName().toLowerCase()).append("\")");
                }
                kts.append("}\n");
            }
            else if (deployment instanceof TarDeployment) {
                String escaped = ((TarDeployment)deployment).outputDir().replace("\\","\\\\");
                addUberJarTask(kts,  "ks", escaped, config);
                List<String> fromFiles = ((TarDeployment)deployment).files();
                if (fromFiles.size() > 0) {
                    String rpmName = ((TarDeployment) deployment).archiveName();
                    kts.append("distributions {\n");
                    kts.append("\tmain {\n");

                    kts.append("\t\tdistributionBaseName.set( \"" + rpmName + "\")\n");
                    kts.append("\t\t contents {\n");
                    for (String file : fromFiles) {
                        String escapedFile = file.replace("\\","\\\\");
                        kts.append("\t\t\tfrom(\"" + escapedFile + "\")\n");
                    }
                    kts.append("\t\t\tfrom(\"" + escaped + "\")\n");
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
        File gradlew = new File(directory, "gradlew.bat");
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            connection.newBuild().setStandardOutput(baos).setStandardError(System.err).forTasks(tasks.toArray(new String[0])).run();
            String output = baos.toString();
            Pattern pattern = Pattern.compile("(Successfully built )(\\w)+");
            Matcher matcher = pattern.matcher(output);
            String dockerId = StringUtils.EMPTY;
            while (matcher.find()){
                String[] words = matcher.group(0).split(" ");
                if (words.length >= 3) {
                    dockerId = words[2];
                }
            }
            final String effDockerId = dockerId;
            System.out.println(output);
            if (StringUtils.isNotEmpty(dockerId)) {
                config.deployments().stream().forEach(
                        d -> {
                            if (d instanceof DockerDeployment)
                                ((DockerDeployment) d).imageId(effDockerId);
                        });
            }
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
                return "Architecture.X86_64";
            case armhf:
                return "Architecture.ARM";
            case arm64:
                return "Architecture.AARCH64";
            case ppc64le:
                return "Architecture.PPC64";
            default:
                throw new RuntimeException("Unknown arch for target: " + t);
        }
    }

    public static String getRpmDebOs(Target t){
        //https://github.com/craigwblake/redline/blob/master/src/main/java/org/redline_rpm/header/Os.java
        switch (t.os()){
            case LINUX:
                return "Os.LINUX";
            case WINDOWS:
                return "Os.CYGWINNT";
            case MACOSX:
                return "Os.MACOSX";
            //case ANDROID:
            default:
                throw new RuntimeException("Unknown os for target: " + t);
        }
    }

    private static void addUberJarTask(StringBuilder kts, String fileName, String directoryName, Config config) {
        kts.append("tasks.withType<ShadowJar> {\n");
        String jarName = fileName;
        kts.append("\tbaseName = \"" + jarName + "\"\n");
        //needed for larger build files, shadowJar
        //extends Jar which extends Zip
        //a lot of documentation on the internet points to zip64 : true
        //as the way to set this, the only way I found to do it in the
        //kotlin dsl was to invoke the setter directly after a bit of reverse engineering
        kts.append("\tsetZip64(true)\n");
        kts.append("\tdestinationDirectory.set(file(\"" + directoryName + "\"))\n");
        kts.append("\tmergeServiceFiles()");  //For service loader files
        kts.append("}\n");

        kts.append("//Add manifest - entry point\n")
                .append("tasks.withType(Jar::class) {\n")
                .append("    manifest {\n")
                .append("        attributes[\"Manifest-Version\"] = \"1.0\"\n")
                .append("        attributes[\"Main-Class\"] = \"ai.konduit.serving.cli.launcher.KonduitServingLauncher\"\n")
                .append("        attributes[\"Konduit-Serving-Build\"] = \"").append(config.toJsonMinimal().replace("\"", "\\\"")).append("\"\n")
                .append("    }\n")
                .append("}\n\n");
    }

    private static void addClassPathTask(StringBuilder kts, ClassPathDeployment cpd, Config config){
        String filePrefix = "file:/" + (SystemUtils.IS_OS_WINDOWS ? "" : "/");

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
            kts.append("//Write a JAR with a manifest containing the path of all dependencies, but no other content\n")
                    .append("tasks.withType(Jar::class) {\n")
                    .append("    manifest {\n")
                    .append("        attributes[\"Manifest-Version\"] = \"1.0\"\n")
                    .append("        attributes[\"Main-Class\"] = \"ai.konduit.serving.cli.launcher.KonduitServingLauncher\"\n")
                    .append("        attributes[\"Class-Path\"] = \"" + filePrefix + "\" + configurations.runtimeClasspath.get().getFiles().joinToString(separator=\" " + filePrefix + "\")\n")
                    .append("        attributes[\"Konduit-Serving-Build\"] = \"").append(config.toJsonMinimal().replace("\"", "\\\"")).append("\"\n")
                    .append("    }\n");

            if(cpd.outputFile() != null){
                String path = cpd.outputFile().replace("\\", "/");
                kts.append("setProperty(\"archiveFileName\", \"").append(path).append("\")\n");
            }

            kts.append("}");
        }
    }

    protected static void copyResource(String resource, File to){
        InputStream is = GradleBuild.class.getResourceAsStream(resource);
        Preconditions.checkState(is != null, "Could not find %s resource that should be available in konduit-serving-build JAR", resource);

        to.getParentFile().mkdirs();

        try(InputStream bis = new BufferedInputStream(is); OutputStream os = new BufferedOutputStream(new FileOutputStream(to))){
            IOUtils.copy(bis, os);
        } catch (IOException e){
            throw new RuntimeException("Error copying resource " + resource + " to " + to, e);
        }
    }
}
