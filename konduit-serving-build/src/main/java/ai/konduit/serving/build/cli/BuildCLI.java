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
import ai.konduit.serving.build.deployments.ClassPathDeployment;
import ai.konduit.serving.build.deployments.UberJarDeployment;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import ai.konduit.serving.build.config.Module;
import java.io.File;
import java.io.IOException;
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
@Name("build")
@Summary("Command line interface for performing Konduit Serving builds.")
@Description("Allows the user to build a JAR or artifact such as a docker image suitable " +
        "for performing inference on a given pipeline on a given deployment target (defined " +
        "as an operating system, CPU architecture and optionally compute device). " +
        "For example, can be used to build for any of the following: \n" +
        "-> HTTP (REST) server on x86 Windows (CPU), packaged as a stand-alone .exe\n" +
        "-> HTTP and GRPC server on CUDA 10.2 + Linux, packaged as a docker image \n" +
        "And many more combinations\n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Creates a deployment for classpath manifest jar for a CPU device:\n" +
        "$ konduit build -dt classpath -c classpath.outputFile=manifest.jar \n" +
        "  classpath.type=jar_manifest -p pipeline.json -d CPU \n\n" +
        "- Creates a uber jar deployment for a CUDA 10.1 device:\n" +
        "$ konduit build -dt classpath -c jar.outputdir=build jar.name=uber.jar \n" +
        "  -p pipeline.json -d CUDA_10.1 \n" +
        "--------------")
@Slf4j
public class BuildCLI extends DefaultCommand {

    public static final String HTTP = "HTTP";
    public static final String GRPC = "GRPC";

    public static final String PIPELINE_OPTION_DESCRIPTION = "Path to a pipeline json file";
    public static final String OS_OPTION_DESCRIPTION = "Operating systems to build for. Valid values: {linux, windows, mac} (case insensitive).\n" +
            "If not set, the current system OS will be used";
    public static final String ARCHITECTURE_OPTION_DESCRIPTION = "The target CPU architecture. Must be one of {x86, x86_avx2, x86_avx512, armhf, arm64, ppc64le}.\n " +
            "Note that most modern desktops can be built with x86_avx2, which is the default";
    public static final String DEVICE_OPTION_DESCRIPTION = "Compute device to be used. If not set: artifacts are build for CPU only.\n" +
            "Valid values: CPU, CUDA_10.0, CUDA_10.1, CUDA_10.2 (case insensitive)";
    public static final String MODULES_OPTION_DESCRIPTION = "Names of the Konduit Serving modules to include, as a comma-separated list of values.\nNote that " +
            "this is not necessary when a pipeline is included (via -p/--pipeline), as the modules will be inferred " +
            "automatically based on the pipeline contents";
    public static final String DEPLOYMENT_TYPE_OPTION_DESCRIPTION = "The deployment types to use: JAR, DOCKER, EXE, WAR, RPM, DEB or TAR (case insensitive)";
    public static final String SERVER_TYPE_OPTION_DESCRIPTION = "Type of server - HTTP or GRPC (case insensitive)";
    public static final String ADDITIONAL_DEPENDENCIES_OPTION_DESCRIPTION = "Additional dependencies to include, in GAV(C) format: \"group_id:artifact_id:version\" / \"group_id:artifact_id:version:classifier\"";
    public static final String CONFIG_OPTION_DESCRIPTION = "Configuration for the deployment types specified via -dt/--deploymentType.\n" +
            "For example, \"-c jar.outputdir=/some/dir jar.name=my.jar\" etc.\n" +
            "Configuration keys:\n" +
            UberJarDeployment.CLI_KEYS + "\n" +
            ClassPathDeployment.CLI_KEYS + "\n";

    @Parameter(names = {"-p", "--pipeline"}, description = PIPELINE_OPTION_DESCRIPTION)
    private String pipeline;

    @Parameter(names = {"-o", "--os"}, validateValueWith = CLIValidators.OSValueValidator.class,
            description = OS_OPTION_DESCRIPTION)
    private List<String> os;

    @Parameter(names = {"-a", "--arch"}, validateValueWith = CLIValidators.ArchValueValidator.class,
            description = ARCHITECTURE_OPTION_DESCRIPTION)
    private String arch = Arch.x86_avx2.toString();

    @Parameter(names = {"-d", "--device"}, validateValueWith = CLIValidators.DeviceValidator.class,
            description = DEVICE_OPTION_DESCRIPTION)
    private String device;

    @Parameter(names = {"-m", "--modules"}, validateValueWith = CLIValidators.ModuleValueValidator.class,
            description = MODULES_OPTION_DESCRIPTION)
    private List<String> modules;

    @Parameter(names = {"-dt", "--deploymentType"}, validateValueWith = CLIValidators.DeploymentTypeValueValidator.class,
            description = DEPLOYMENT_TYPE_OPTION_DESCRIPTION)
    private List<String> deploymentTypes = Collections.singletonList(Deployment.JAR);

    @Parameter(names = {"-s", "--serverType"},
            description = SERVER_TYPE_OPTION_DESCRIPTION,
            validateValueWith = CLIValidators.ServerTypeValidator.class)
    private List<String> serverTypes = Arrays.asList(HTTP, GRPC);

    @Parameter(names = {"-ad", "--addDep"},
            description = ADDITIONAL_DEPENDENCIES_OPTION_DESCRIPTION,
            validateValueWith = CLIValidators.AdditionalDependenciesValidator.class)
    private List<String> additionalDependencies;

    @Parameter(names = {"-c", "--config"},
            description = CONFIG_OPTION_DESCRIPTION,
            variableArity = true,
            validateValueWith = CLIValidators.ConfigValidator.class)
    private List<String> config;

    @Parameter(names = {"-h", "--help"}, help = true, arity = 0)
    private boolean help;

    @Option(shortName = "p", longName = "pipeline")
    @Description(PIPELINE_OPTION_DESCRIPTION)
    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    @Option(shortName = "o", longName = "os", acceptMultipleValues = true)
    @Description(OS_OPTION_DESCRIPTION)
    public void setOperatingSystem(List<String> operatingSystem) {
        try {
            new CLIValidators.OSValueValidator().validate("os", operatingSystem);
        } catch (Exception e) {
            out.println("Error validating OS (-o/--os): " + e.getMessage());
            System.exit(1);
        }
        this.os = operatingSystem;
    }

    @Option(shortName = "a", longName = "arch")
    @Description(ARCHITECTURE_OPTION_DESCRIPTION)
    public void setArchitecture(String architecture) {
        try {
            new CLIValidators.ArchValueValidator().validate("arch", architecture);
        } catch (Exception e) {
            out.println("Error validating architecture (-a/--arch): " + e.getMessage());
            System.exit(1);
        }
        this.arch = architecture;
    }

    @Option(shortName = "d", longName = "device")
    @Description(DEVICE_OPTION_DESCRIPTION)
    public void setDevice(String device) {
        try {
            new CLIValidators.DeviceValidator().validate("device", device);
        } catch (Exception e) {
            out.println("Error validating device (-d/--device): " + e.getMessage());
            System.exit(1);
        }
        this.device = device;
    }

    @Option(shortName = "m", longName = "modules", acceptMultipleValues = true)
    @Description(MODULES_OPTION_DESCRIPTION)
    public void setModules(List<String> modules) {
        try {
            new CLIValidators.ModuleValueValidator().validate("modules", modules);
        } catch (Exception e) {
            out.println("Error validating modules (-m/--modules): " + e.getMessage());
            System.exit(1);
        }
        this.modules = modules;
    }

    @Option(shortName = "dt", longName = "deploymentType", acceptMultipleValues = true)
    @Description(DEPLOYMENT_TYPE_OPTION_DESCRIPTION)
    public void setDeploymentTypes(List<String> deploymentTypes) {
        try {
            new CLIValidators.DeploymentTypeValueValidator().validate("deploymentType", deploymentTypes);
        } catch (Exception e) {
            out.println("Error validating OS (-dt/--deploymentType): " + e.getMessage());
            System.exit(1);
        }
        this.deploymentTypes = deploymentTypes;
    }

    @Option(shortName = "s", longName = "serverType", acceptMultipleValues = true)
    @Description(SERVER_TYPE_OPTION_DESCRIPTION)
    public void setServerTypes(List<String> serverTypes) {
        try {
            new CLIValidators.ServerTypeValidator().validate("serverType", serverTypes);
        } catch (Exception e) {
            out.println("Error validating server type (-s/--serverType): " + e.getMessage());
            System.exit(1);
        }
        this.serverTypes = serverTypes;
    }

    @Option(shortName = "ad", longName = "addDep", acceptMultipleValues = true)
    @Description(ADDITIONAL_DEPENDENCIES_OPTION_DESCRIPTION)
    public void setAdditionalDependencies(List<String> additionalDependencies) {
        try {
            new CLIValidators.AdditionalDependenciesValidator().validate("additionalDependencies", additionalDependencies);
        } catch (Exception e) {
            out.println("Error validating additional dependencies (-a/--addDep): " + e.getMessage());
            System.exit(1);
        }
        this.additionalDependencies = additionalDependencies;
    }

    @Option(shortName = "c", longName = "config", acceptMultipleValues = true)
    @Description(CONFIG_OPTION_DESCRIPTION)
    public void setConfig(List<String> config) {
        try {
            new CLIValidators.ConfigValidator().validate("config", config);
        } catch (Exception e) {
            out.println("Error validating config (-c/--config): " + e.getMessage());
            System.exit(1);
        }
        this.config = config;
    }

    public static void main(String... args) throws Exception {
        new BuildCLI().exec(args);
    }

    public void exec(String[] args) {
        JCommander jCommander = new JCommander();
        jCommander.addObject(this);
        jCommander.parse(args);

        if(help) {
            jCommander.usage();
            return;
        }

        run();
    }

    @Override
    public void run() throws CLIException {
        if (out == null) {
            out = System.out;
        }

        //Infer OS if necessary
        if(os == null || os.isEmpty())
            inferOS();

        //------------------------------------- Build Configuration --------------------------------------

        //Print out configuration / values
        int width = 96;
        int keyWidth = 30;
        out.println(padTo("Konduit Serving Build Tool", '=', width));
        out.println(padTo("Build Configuration", '-', width));
        out.println(padRight("Pipeline:", ' ', keyWidth) + (pipeline == null ? "<not specified>" : pipeline));
        out.println(padRight("Target OS:", ' ', keyWidth) + (os.size() == 1 ? os.get(0) : os.toString()));
        out.println(padRight("Target CPU arch.:", ' ', keyWidth) + arch);
        out.println(padRight("Target Device:", ' ', keyWidth) + (device == null ? "CPU" : device));
        if(modules != null){
            out.println(padRight("Additional modules:", ' ', keyWidth) + String.join(", ", modules));
        }
        out.println(padRight("Server type(s):", ' ', keyWidth) + String.join(", ", serverTypes));
        out.println(padRight("Deployment type(s):", ' ', keyWidth) + String.join(", ", deploymentTypes));
        if(additionalDependencies != null){
            out.println(padRight("Additional dependencies:", ' ', keyWidth) + String.join(", ", additionalDependencies));
        }
        out.println("\n");

        Map<String,String> propsIn = new HashMap<>();

        if(config != null){
            for(String s : config){
                String[] split = s.split("=");
                propsIn.put(split[0], split[1]);
            }
        }

        List<Deployment> deployments = parseDeployments(propsIn);
        for( int i=0; i<deployments.size(); i++ ){
            Deployment d = deployments.get(i);
            if(deployments.size() > 1){
                out.println("Deployment " + (i+1) + " of " + deployments.size() + " configuration: " + d.getClass().getSimpleName());
            } else {
                out.println("Deployment configuration: " + d.getClass().getSimpleName());
            }
            Map<String,String> props = d.asProperties();
            for(Map.Entry<String,String> e : props.entrySet()){
                out.println(padRight("  " + e.getKey() + ":", ' ', keyWidth) + e.getValue());
            }
        }

        out.println("\n");

        //--------------------------------------- Validating Build ---------------------------------------
        out.println(padTo("Validating Build", '-', width));

        if((pipeline == null || pipeline.isEmpty()) && (modules == null || modules.isEmpty())){
            String s = "BUILD FAILED: Either a path to a Pipeline (JSON or YAML) must be provided via -p/--pipeline" +
                    " or a list of modules to include must be provided via -m/--modules." +
                    " When a pipeline is provided via JSON or YAML, the required modules will be determined automatically.";
            out.println(wrapString(s, width));
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

        int width2 = 36;
        if(pipeline != null){
            out.println("Resolving modules required for pipeline execution...");
            List<Module> resolvedModules = c.resolveModules();
            for(Module m : resolvedModules){
                out.println("  " + m.name());
            }
            out.println();

            if(modules != null && !modules.isEmpty()){
                out.println("Additional modules specified:");
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
                            out.println("  " + m.name());
                            toAdd.add(m);
                        }
                    } else {
                        anyFailed = true;
                        out.println("  " + s);
                        failed.add(s);
                    }
                }
                if(anyFailed){
                    out.println("Failed to resolve modules specified via -m/--modules: " + failed);
                    if(failed.size() == 1){
                        out.println("No module is known with this name: " + failed.get(0) );
                    } else {
                        out.println("No modules are known with these names: " + failed );
                    }
                    System.exit(1);
                }

                c.addModules(toAdd);
                resolvedModules = c.modules();

                out.println();
            }

            List<Dependency> d = c.resolveDependencies();
            out.println("Resolving module optional/configurable dependencies for deployment target: " + t);
            boolean anyFail = false;
            for(Module m : resolvedModules){
                ModuleRequirements r = m.dependencyRequirements();
                boolean satisfied = r == null || r.satisfiedBy(t, d);
                String s = padRight("  " + m.name() + ":", ' ', width2);
                out.println(s + (satisfied ? " OK" : " FAILED TO RESOLVE REQUIRED DEPENDENCY FOR OS + TARGET ARCHITECTURE"));
                if(!satisfied){
                    anyFail = true;
                    List<DependencyRequirement> l = r.reqs();
                    for(DependencyRequirement dr : l){
                        if(dr.satisfiedBy(t, d)){
                            out.println("      OK:     " + dr);
                        } else {
                            out.println("      FAILED: " + dr);
                        }
                    }
                }
            }
            if(anyFail){
                out.println("BUILD FAILED: Unable to resolve optional dependencies for one or more modules");
                out.println("This likely suggests the module dependencies do not support the target + architecture combination");
                System.exit(1);
            }

            out.println();

            if(!d.isEmpty()){
                out.println("Resolved dependencies:");
                for(Dependency dep : d){
                    out.println("  " + dep.gavString());
                }
            }
            out.println();

            out.println("Checking deployment configurations:");
            boolean anyDeploymentsFailed = false;
            for(Deployment dep : deployments){
                DeploymentValidation v = dep.validate();
                String s = dep.getClass().getSimpleName();
                String s2 = padRight("  " + s + ":", ' ', width2);
                out.println(s2 + (v.ok() ? "OK" : "FAILED"));
                if(!v.ok()){
                    anyDeploymentsFailed = true;
                    for(String f : v.failureMessages()){
                        out.println("    " + f);
                    }
                }
            }

            if(anyDeploymentsFailed){
                out.println("BUILD FAILED: one or more deployment method configurations failed.");
                out.println("See failure messages above for details");
                System.exit(1);
            }

            out.println("\n>> Validation Passed\n");
        }


        //-------------------------------------------- Build ---------------------------------------------

        out.println(padTo("Build", '-', width));
        File tempDir = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());

        out.println("Generating build files...");
        try {
            GradleBuild.generateGradleBuildFiles(tempDir, c);
        } catch (IOException cause) {
            throw new CLIException("Failed to generate gradle build files.", cause);
        }
        out.println(">> Build file generation complete\n\n");

        out.println("Starting build...");
        long start = System.currentTimeMillis();
        try {
            GradleBuild.runGradleBuild(tempDir, c);
        } catch (IOException cause) {
            throw new CLIException("Gradle build failed", cause);
        }
        long end = System.currentTimeMillis();

        out.println(">> Build complete\n\n");




        out.println(padTo("Build Summary", '-', width));
        out.println(padRight("Build duration:", ' ', keyWidth) + (end-start)/1000 + " sec");
        out.println(padRight("Output artifacts:", ' ', keyWidth) + deployments.size());

        out.println();

        for(Deployment d : deployments){
            out.println(" ----- " + d.getClass().getSimpleName() + " -----");
            out.println(d.outputString());
            out.println();
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


    public List<Deployment> parseDeployments(Map<String, String> props){
        List<Deployment> out = new ArrayList<>();
        for(String s : deploymentTypes){
            switch (s){
                case Deployment.CLASSPATH:
                    ClassPathDeployment classPathDeployment =
                            new ClassPathDeployment().type(ClassPathDeployment.Type.JAR_MANIFEST).outputFile("manifest.jar");
                    classPathDeployment.fromProperties(props);
                    out.add(classPathDeployment);
                    break;
                case Deployment.JAR:
                case Deployment.UBERJAR:
                    UberJarDeployment uberJarDeployment = new UberJarDeployment().outputDir(new File("").getAbsolutePath());
                    uberJarDeployment.fromProperties(props);
                    out.add(uberJarDeployment);
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
