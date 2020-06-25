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

package ai.konduit.serving.build.cli;

import ai.konduit.serving.build.build.GradleBuild;
import ai.konduit.serving.build.config.*;
import ai.konduit.serving.build.config.Arch;
import ai.konduit.serving.build.config.OS;
import ai.konduit.serving.build.config.Target;
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.dependencies.DependencyRequirement;
import ai.konduit.serving.build.dependencies.ModuleRequirements;
import ai.konduit.serving.build.deployments.ClassPathDeployment;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import ai.konduit.serving.build.config.Module;
import ai.konduit.serving.pipeline.api.protocol.URIResolver;
import com.beust.jcommander.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.*;

/**
 * Command line interface for performing Konduit Serving builds
 * Allows the user to build a JAR or artifact such as a docker image suitable for performing inference on a given
 * pipeline on a given deployment target (defined as an operating system, CPU architecture and optionally compute device).<br>
 * <br>
 * For example, can be used to build for any of the following:
 * * HTTP (REST) server on x86 Windows (CPU), packaged as a stand-alone .exe<br>
 * * HTTP and GRPC server on CUDA 10.2 + Linux, packaged as a docker image<br>
 * And many more combinations
 *
 *
 * @author Alex black
 */
@Slf4j
public class BuildCLI {

    public static final String HTTP = "HTTP";
    public static final String GRPC = "GRPC";



    @Parameter(names = {"-p", "--pipeline"})
    private String pipeline;

    @Parameter(names = {"-o", "--os"}, validateValueWith = CLIValidators.OSValueValidator.class,
            description = "Operating systems to build for. Valid values: {linux, windows, mac} (case insensitive).\n" +
                    "If not set, the current system OS will be used")
    private List<String> os;

    @Parameter(names = {"-a", "--arch"}, validateValueWith = CLIValidators.ArchValueValidator.class,
            description = "The target CPU architecture. Must be one of {x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le}.\n " +
                    "Note that most modern desktops can be built with x86_avx2, which is the default")
    private String arch = Arch.x86_avx2.toString();

    @Parameter(names = {"-d", "--device"}, validateValueWith = CLIValidators.DeviceValidator.class,
            description = "Compute device to be used. If not set: artifacts are build for CPU only.\n" +
                    "Valid values: CPU, CUDA_10.0, CUDA_10.1, CUDA_10.2 (case insensitive)")
    private String device;

    @Parameter(names = {"-m", "--modules"}, validateValueWith = CLIValidators.ModuleValueValidator.class,
            description = "Names of the Konduit Serving modules to include, as a comma-separated list of values.\nNote that " +
                    "this is not necessary when a pipeline is included (via -p/--pipeline), as the modules will be inferred " +
                    "automatically based on the pipeline contents")
    private List<String> modules;

    @Parameter(names = {"-dt", "--deploymentType"}, validateValueWith = CLIValidators.DeploymentTypeValueValidator.class,
            description = "The deployment types to use: JAR, DOCKER, EXE, WAR, RPM, DEB or TAR (case insensitive)")
    private List<String> deploymentTypes = Collections.singletonList(Deployment.JAR);

    @Parameter(names = {"-s", "--serverType"},
            description = "Type of server - HTTP or GRPC (case insensitive)",
            validateValueWith = CLIValidators.ServerTypeValidator.class)
    private List<String> serverTypes = Arrays.asList(HTTP, GRPC);

    @Parameter(names = {"-ad", "--additionalDependencies"},
            description = "Additional dependencies to include, in GAV(C) format: \"group_id:artifact_id:version\" / \"group_id:artifact_id:version:classifier\"",
            validateValueWith = CLIValidators.AdditionalDependenciesValidator.class)
    private List<String> additionalDependencies;

    @Parameter(names = {"-c", "--config"},
            description = "Configuration for the deployment types specified via -dt/--deploymentType.\n" +
                    "For example, \"-c jar.outputdir=/some/dir jar.name=my.jar\" etc.\n" +
                    "Configuration keys:\n" +
                    UberJarDeployment.CLI_KEYS + "\n" +
                    ClassPathDeployment.CLI_KEYS + "\n",
            variableArity = true,
            validateValueWith = CLIValidators.ConfigValidator.class)
    private List<String> config;

    public static void main(String... args) throws Exception {
        new BuildCLI().exec(args);
    }

    public void exec(String[] args) throws Exception {
        JCommander.newBuilder().addObject(this).build().parse(args);

        //Infer OS if necessary
        if(os == null || os.isEmpty())
            inferOS();


        //------------------------------------- Build Configuration --------------------------------------

        //Print out configuration / values
        int width = 96;
        int keyWidth = 30;
        System.out.println(padTo("Konduit Serving Build Tool", '=', width));
        System.out.println(padTo("Build Configuration", '-', width));
        System.out.println(padRight("Pipeline:", ' ', keyWidth) + (pipeline == null ? "<not specified>" : pipeline));
        System.out.println(padRight("Target OS:", ' ', keyWidth) + (os.size() == 1 ? os.get(0) : os.toString()));
        System.out.println(padRight("Target CPU arch.:", ' ', keyWidth) + arch);
        System.out.println(padRight("Target Device:", ' ', keyWidth) + (device == null ? "CPU" : device));
        if(modules != null){
            System.out.println(padRight("Additional modules:", ' ', keyWidth) + String.join(", ", modules));
        }
        System.out.println(padRight("Server type(s):", ' ', keyWidth) + String.join(", ", serverTypes));
        System.out.println(padRight("Deployment type(s):", ' ', keyWidth) + String.join(", ", deploymentTypes));
        if(additionalDependencies != null){
            System.out.println(padRight("Additional dependencies:", ' ', keyWidth) + String.join(", ", additionalDependencies));
        }
        System.out.println("\n");

        List<Deployment> deployments = parseDeployments();
        for( int i=0; i<deployments.size(); i++ ){
            Deployment d = deployments.get(i);
            if(deployments.size() > 1){
                System.out.println("Deployment " + (i+1) + " of " + deployments.size() + " configuration: " + d.getClass().getSimpleName());
            } else {
                System.out.println("Deployment configuration: " + d.getClass().getSimpleName());
            }
            Map<String,String> props = d.asProperties();
            for(Map.Entry<String,String> e : props.entrySet()){
                System.out.println(padRight("  " + e.getKey() + ":", ' ', keyWidth) + e.getValue());
            }
        }

        System.out.println("\n");

        //--------------------------------------- Validating Build ---------------------------------------
        System.out.println(padTo("Validating Build", '-', width));

        if((pipeline == null || pipeline.isEmpty()) && (modules == null || modules.isEmpty())){
            String s = "BUILD FAILED: Either a path to a Pipeline (JSON or YAML) must be provided via -p/--pipeline" +
                    " or a list of modules to include must be provided via -m/--modules." +
                    " When a pipeline is provided via JSON or YAML, the required modules will be determined automatically.";
            System.out.println(wrapString(s, width));
            System.exit(1);
        }

        ComputeDevice cd = device == null ? null : ComputeDevice.forName(device);
        Arch a = Arch.forName(arch);
        Target t = new Target(OS.forName(os.get(0)), a, cd);

        //Parse server type
        List<Serving> serving = new ArrayList<>();
        for(String s : serverTypes){
            serving.add(Serving.valueOf(s.toUpperCase()));
        }


        Config c = new Config()
                .pipelinePath(pipeline)
                .target(t)
                .deployments(deployments)
                .serving(serving)
                .additionalDependencies(additionalDependencies);

        if(config != null){
            Map<String,String> props = new HashMap<>();
            for(String s : config){
                File localFile = URIResolver.getFile(s);
                if (localFile.exists()) {
                    String content = FileUtils.readFileToString(localFile, "UTF-8");
                    String[] split = content.split("=");
                    props.put(split[0], split[1]);
                }
            }
            for(Deployment d : deployments){
                d.fromProperties(props);
            }
        }


        int width2 = 36;
        if(pipeline != null){
            System.out.println("Resolving modules required for pipeline execution...");
            List<Module> resolvedModules = c.resolveModules();
            for(Module m : resolvedModules){
                System.out.println("  " + m.name());
            }
            System.out.println();

            if(modules != null && !modules.isEmpty()){
                System.out.println("Additional modules specified:");
                List<Module> toAdd = new ArrayList<>();
                boolean anyFailed = false;
                List<String> failed = new ArrayList<>();
                for(String s : modules){
                    boolean e1 = Module.moduleExistsForName(s, false);
                    boolean e2 = Module.moduleExistsForName(s, true);
                    if(e1 || e2){
                        Module m = e1 ? Module.forName(s) : Module.forShortName(s);
                        if(resolvedModules.contains(m)){
                            //Already resolved this one
                            continue;
                        } else {
                            System.out.println("  " + m.name());
                            toAdd.add(m);
                        }
                    } else {
                        anyFailed = true;
                        System.out.println("  " + s);
                        failed.add(s);
                    }
                }
                if(anyFailed){
                    System.out.println("Failed to resolve modules specified via -m/--modules: " + failed);
                    if(failed.size() == 1){
                        System.out.println("No module is known with this name: " + failed.get(0) );
                    } else {
                        System.out.println("No modules are known with these names: " + failed );
                    }
                    System.exit(1);
                }

                c.addModules(toAdd);
                resolvedModules = c.modules();

                System.out.println();
            }

            List<Dependency> d = c.resolveDependencies();
            System.out.println("Resolving module optional/configurable dependencies for deployment target: " + t);
            boolean anyFail = false;
            for(Module m : resolvedModules){
                ModuleRequirements r = m.dependencyRequirements();
                boolean satisfied = r == null || r.satisfiedBy(t, d);
                String s = padRight("  " + m.name() + ":", ' ', width2);
                System.out.println(s + (satisfied ? " OK" : " FAILED TO RESOLVE REQUIRED DEPENDENCY FOR OS + TARGET ARCHITECTURE"));
                if(!satisfied){
                    anyFail = true;
                    List<DependencyRequirement> l = r.reqs();
                    for(DependencyRequirement dr : l){
                        if(dr.satisfiedBy(t, d)){
                            System.out.println("      OK:     " + dr);
                        } else {
                            System.out.println("      FAILED: " + dr);
                        }
                    }
                }
            }
            if(anyFail){
                System.out.println("BUILD FAILED: Unable to resolve optional dependencies for one or more modules");
                System.out.println("This likely suggests the module dependencies do not support the target + architecture combination");
                System.exit(1);
            }

            System.out.println();

            if(!d.isEmpty()){
                System.out.println("Resolved dependencies:");
                for(Dependency dep : d){
                    System.out.println("  " + dep.gavString());
                }
            }
            System.out.println();

            System.out.println("Checking deployment configurations:");
            boolean anyDeploymentsFailed = false;
            for(Deployment dep : deployments){
                DeploymentValidation v = dep.validate();
                String s = dep.getClass().getSimpleName();
                String s2 = padRight("  " + s + ":", ' ', width2);
                System.out.println(s2 + (v.ok() ? "OK" : "FAILED"));
                if(!v.ok()){
                    anyDeploymentsFailed = true;
                    for(String f : v.failureMessages()){
                        System.out.println("    " + f);
                    }
                }
            }

            if(anyDeploymentsFailed){
                System.out.println("BUILD FAILED: one or more deployment method configurations failed.");
                System.out.println("See failure messages above for details");
                System.exit(1);
            }

            System.out.println("\n>> Validation Passed\n");
        }


        //-------------------------------------------- Build ---------------------------------------------

        System.out.println(padTo("Build", '-', width));
        File tempDir = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());

        System.out.println("Generating build files...");
        GradleBuild.generateGradleBuildFiles(tempDir, c);
        System.out.println(">> Build file generation complete\n\n");

        System.out.println("Starting build...");
        long start = System.currentTimeMillis();
        GradleBuild.runGradleBuild(tempDir, c);
        long end = System.currentTimeMillis();

        System.out.println(">> Build complete\n\n");




        System.out.println(padTo("Build Summary", '-', width));
        System.out.println(padRight("Build duration:", ' ', keyWidth) + (end-start)/1000 + " sec");
        System.out.println(padRight("Output artifacts:", ' ', keyWidth) + deployments.size());

        System.out.println();

        for(Deployment d : deployments){
            System.out.println(" ----- " + d.getClass().getSimpleName() + " -----");
            System.out.println(d.outputString());
            System.out.println();
        }
    }

    private String padTo(String in, char padChar, int toLength){
        if(in.length() + 2 >= toLength){
            return in;
        }

        int toAdd = toLength - in.length() - 2; //-2 for spaces
        int before = toAdd / 2;
        int after = toAdd - before;
        StringBuilder sb = new StringBuilder();
        for( int i=0; i<before; i++ ){
            sb.append(padChar);
        }
        sb.append(" ").append(in).append(" ");
        for( int i=0; i<after; i++ ){
            sb.append(padChar);
        }

        return sb.toString();
    }

    private String padRight(String in, char padChar, int toLength){
        if(in.length() >= toLength)
            return in;
        StringBuilder sb = new StringBuilder();
        sb.append(in);
        for(int i=0; i<toLength-in.length(); i++ ){
            sb.append(padChar);
        }
        return sb.toString();
    }

    private String wrapString(String in, int maxLength){
        if(in.length() <= maxLength)
            return in;
        StringBuilder sb = new StringBuilder();
        String[] split = in.split(" ");
        int lengthCurrLine = 0;
        for(String s : split){
            if(lengthCurrLine + s.length() + 1 >= maxLength){
                sb.append("\n");
                lengthCurrLine = 0;
            }
            if(lengthCurrLine > 0) {
                sb.append(" ");
                lengthCurrLine++;
            }
            sb.append(s);
            lengthCurrLine += s.length();
        }
        return sb.toString();
    }


    public List<Deployment> parseDeployments(){
        //TODO we need to have configuration
        List<Deployment> out = new ArrayList<>();
        for(String s : deploymentTypes){
            switch (s){
                case Deployment.JAR:
                case Deployment.UBERJAR:
                    File f = new File("");
                    out.add(new UberJarDeployment(f.getAbsolutePath()));
                    break;
                default:
                    throw new RuntimeException("Deployment type not yet implemented: " + s);
            }
        }
        return out;
    }

    protected void inferOS(){
        if(SystemUtils.IS_OS_LINUX) {
            os = Collections.singletonList(OS.LINUX.name());
        } else if(SystemUtils.IS_OS_WINDOWS){
            os = Collections.singletonList(OS.WINDOWS.name());
        } else  if(SystemUtils.IS_OS_MAC){
            os = Collections.singletonList(OS.MACOSX.name());
        } else {
            throw new IllegalStateException("No OS was provided and operating system could not be inferred");
        }
    }
}
