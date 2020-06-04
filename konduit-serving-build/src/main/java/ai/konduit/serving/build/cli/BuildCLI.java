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
import ai.konduit.serving.build.dependencies.Dependency;
import ai.konduit.serving.build.dependencies.DependencyRequirement;
import ai.konduit.serving.build.dependencies.ModuleRequirements;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import com.beust.jcommander.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class BuildCLI {

    public static final String JAR = "JAR";
    public static final String UBERJAR = "UBERJAR";
    public static final String DOCKER = "DOCKER";
    public static final String EXE = "EXE";
    public static final String WAR = "WAR";
    public static final String RPM = "RPM";
    public static final String DEB = "DEB";
    public static final String TAR = "TAR";


    @Parameter(names = {"-p", "--pipeline"})
    private String pipeline;

    @Parameter(names = {"-o", "--os"}, required = true,
            description = "Operating systems to build for. Valid values: {linux, windows, mac} (case insensitive)",
            validateValueWith = CLIValidators.OSValueValidator.class)
    private List<String> os;

    @Parameter(names = {"-a", "--arch"},
            description = "The target CPU architecture. Must be one of {x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le}.\n " +
                    "Note that most modern desktops can be built with x86_avx2, which is the default",
            validateValueWith = CLIValidators.ArchValueValidator.class)
    private String arch = Target.Arch.x86_avx2.toString();

    @Parameter(names = {"-d", "--device"},
            description = "")
    private String device;

    @Parameter(names = {"-m", "--modules"},
            description = "Names of the Konduit Serving modules to include, as a comma-separated list of values.\nNote that " +
                    "this is not necessary when a pipeline is included (via -p/--pipeline), as the modules will be inferred " +
                    "automatically based on the pipeline contents",
            validateValueWith = CLIValidators.ModuleValueValidator.class
    )
    private List<String> modules;

    @Parameter(names = {"-dt", "--deploymentType"},
            description = "",
            validateValueWith = CLIValidators.DeploymentTypeValueValidator.class
    )
    private List<String> deploymentTypes = Collections.singletonList(Deployment.JAR);


    public static void main(String... args) throws Exception {
        new BuildCLI().exec(args);
    }

    public void exec(String[] args) throws Exception {
        JCommander.newBuilder().addObject(this).build().parse(args);

        Pipeline p = null;
        if(pipeline != null){
            File f = new File(pipeline);
            if(!f.exists() || !f.isFile()){
                System.out.println("Provided pipeline (via -p or --pipeline) does not exist: " + pipeline);
                System.exit(1);
            }

            String s = FileUtils.readFileToString(new File(pipeline), StandardCharsets.UTF_8);
            try {
                p = Pipeline.fromJson(s);
            } catch (Throwable t){
                try{
                    p = Pipeline.fromYaml(s);
                } catch (Throwable t2){
                    System.out.println("Could not parse Pipeline file as either JSON or YAML");
                    System.out.println("JSON error:");
                    t.printStackTrace();
                    System.out.println("YAML error:");
                    t2.printStackTrace();
                    System.exit(1);
                }
            }
        }



        //Print out values
        int width = 96;
        int keyWidth = 30;
        System.out.println(padTo("Konduit Serving Build Tool", '=', width));
        System.out.println(padTo("Build Configuration", '-', width));
        System.out.println(padRight("Pipeline:", ' ', keyWidth) + (pipeline == null ? "<not specified>" : pipeline));
        System.out.println(padRight("Target OS:", ' ', keyWidth) + (os.size() == 1 ? os.get(0) : os.toString()));
        System.out.println(padRight("Target CPU arch.:", ' ', keyWidth) + arch);
        System.out.println(padRight("Target Device:", ' ', keyWidth) + (device == null ? "CPU" : device));
        if(modules != null){
            System.out.println(padRight("Specified modules:", ' ', keyWidth) + (device == null ? "CPU" : device));
        }
        System.out.println(padRight("Deployment types:", ' ', keyWidth) + deploymentTypes);
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

        System.out.println(padTo("Validating Build", '-', width));

        if((pipeline == null || pipeline.isEmpty()) && (modules == null || modules.isEmpty())){
            String s = "BUILD FAILED: Either a path to a Pipeline (JSON or YAML) must be provided via -p/--pipeline" +
                    " or a list of modules to include must be provided via -m/--modules." +
                    " When a pipeline is provided via JSON or YAML, the required modules will be determined automatically.";
            System.out.println(wrapString(s, width));
            System.exit(1);
        }

        ComputeDevice cd = device == null ? null : ComputeDevice.forName(device);
        Target.Arch a = Target.Arch.forName(arch);
        Target t = new Target(Target.OS.forName(os.get(0)), a, cd);




        Config c = new Config();
        c.pipelinePath(pipeline);
        c.target(t);
        c.deployments(deployments);
        int width2 = 36;
        if(pipeline != null){
            System.out.println("Resolving modules required for pipeline execution...");
            List<Module> resolvedModules = c.resolveModules();
            for(Module m : resolvedModules){
                System.out.println("  " + m.name());
            }

            List<Dependency> d = c.resolveDependencies();
            System.out.println("Resolving module optional/configurable dependencies given target: " + t);
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

            if(!d.isEmpty()){
                System.out.println("Resolved dependencies:");
                for(Dependency dep : d){
                    System.out.println("  " + dep.gavString());
                }
            }

            System.out.println("\n>> Validation Passed\n");
        }



        System.out.println(padTo("Starting Build", '-', width));
        File tempDir = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());

        System.out.println("Generating build files...");
        GradleBuild.generateGradleBuildFiles(tempDir, c);
        System.out.println(" COMPLETE");

        System.out.println("Starting build...");
        GradleBuild.runGradleBuild(tempDir);


        System.out.println(padTo("Build Complete", '=', width));
        for(Deployment d : deployments){
            System.out.println(" ----- " + d.getClass().getSimpleName() + " -----");
            System.out.println(d.outputString());
            System.out.println();
        }


        /*
        Also need to check:
        - that HTTP or GRPC is set (not null)
        - Output dir for uber-jar is set
        - At least one deployment type is set
        - Generally validation for any deployment type

         */
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

}
