/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.launcher.command;

import io.vertx.core.cli.annotations.*;
import io.vertx.core.impl.launcher.CommandLineUtils;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import io.vertx.core.impl.launcher.commands.StartCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Name("serve")
@Summary("Start a konduit server application in background")
@Description("Start a konduit server application as a background service. " +
        "The application is identified with an id that can be set using the `--serving-id` or `-id` option. " +
        "The application can be stopped with the `stop` command. " +
        "This command takes the `run` command parameters. To see the " +
        "run command parameters, execute `run --help`")
@Slf4j
public class ServeCommand extends StartCommand {

    private String id;
    private String launcher;

    private boolean redirect;
    private String jvmOptions;

    /**
     * Sets the "application id" that would be to stop the application and be listed in the {@link ListCommand} command.
     *
     * @param id the application ID.
     */
    @Option(longName = "serving-id", shortName = "id", required = true)
    @Description("Id of the serving process. This will be visible in the 'list' command. This id can be used to call 'predict' and 'stop' commands on the running servers.")
    public void setApplicationId(String id) {
        this.id = id;
    }

    /**
     * Sets the Java Virtual Machine options to pass to the spawned process. If not set, the JAVA_OPTS environment
     * variable is used.
     *
     * @param options the jvm options
     */
    @Option(longName = "java-opts")
    @Description("Java Virtual Machine options to pass to the spawned process such as \"-Xmx1G -Xms256m " +
            "-XX:MaxPermSize=256m\". If not set the `JAVA_OPTS` environment variable is used.")
    public void setJavaOptions(String options) {
        this.jvmOptions = options;
    }

    /**
     * A hidden option to set the launcher class.
     *
     * @param clazz the class
     */
    @Option(longName = "launcher-class")
    @Hidden
    public void setLauncherClass(String clazz) {
        this.launcher = clazz;
    }

    /**
     * Whether the created process error streams and output streams needs to be redirected to the launcher process.
     *
     * @param redirect {@code true} to enable redirection, {@code false} otherwise.
     */
    @Option(longName = "redirect", flag = true)
    @Description("Whether the created process error streams and output streams needs to be redirected to the launcher process.")
    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * Starts the application in background.
     */
    @Override
    public void run() {
        out.println("Starting konduit server...");
        List<String> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        addJavaCommand(cmd);

        // Must be called only once !
        List<String> cliArguments = getArguments();

        // Add the classpath to env.
        builder.environment().put("CLASSPATH", System.getProperty("java.class.path"));

        if (launcher != null) {
            ExecUtils.addArgument(cmd, launcher);
            // Do we have a valid command ?
            Optional<String> maybeCommand = cliArguments.stream()
                    .filter(arg -> executionContext.launcher().getCommandNames().contains(arg))
                    .findFirst();
            if (! maybeCommand.isPresent()) {
                // No command, add `run`
                ExecUtils.addArgument(cmd, "run");
            }
        } else if (isLaunchedAsFatJar()) {
            ExecUtils.addArgument(cmd, "-jar");
            ExecUtils.addArgument(cmd, CommandLineUtils.getJar());
        } else {
            // probably a `vertx` command line usage, or in IDE.
            ExecUtils.addArgument(cmd, CommandLineUtils.getFirstSegmentOfCommand());
            ExecUtils.addArgument(cmd, "run");
        }

        cliArguments.forEach(arg -> ExecUtils.addArgument(cmd, arg));

        try {
            builder.command(cmd);
            if (redirect) {
                builder.redirectError(ProcessBuilder.Redirect.INHERIT);
                builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            builder.start();
            out.println(id);
        } catch (Exception e) {
            out.println("Cannot create konduit server process");
            e.printStackTrace(out);
            ExecUtils.exitBecauseOfProcessIssue();
        }

    }

    private void addJavaCommand(List<String> cmd) {
        if (ExecUtils.isWindows()) {
            ExecUtils.addArgument(cmd, "cmd.exe");
            ExecUtils.addArgument(cmd, "/C");
            ExecUtils.addArgument(cmd, "start");
            ExecUtils.addArgument(cmd, "serving-id - " + id);
            ExecUtils.addArgument(cmd, "/B");
        }
        ExecUtils.addArgument(cmd, getJava().getAbsolutePath());

        // Compute JVM Options
        if (jvmOptions == null) {
            String opts = System.getenv("JAVA_OPTS");
            if (opts != null) {
                Arrays.stream(opts.split(" ")).forEach(s -> ExecUtils.addArgument(cmd, s));
            }
        } else {
            Arrays.stream(jvmOptions.split(" ")).forEach(s -> ExecUtils.addArgument(cmd, s));
        }
    }

    private File getJava() {
        File java;
        File home = new File(System.getProperty("java.home"));
        if (ExecUtils.isWindows()) {
            java = new File(home, "bin/java.exe");
        } else {
            java = new File(home, "bin/java");
        }

        if (!java.isFile()) {
            out.println("Cannot find java executable - " + java.getAbsolutePath() + " does not exist");
            ExecUtils.exitBecauseOfSystemConfigurationIssue();
        }
        return java;
    }

    private boolean isLaunchedAsFatJar() {
        return CommandLineUtils.getJar() != null;
    }

    private List<String> getArguments() {
        List<String> args = executionContext.commandLine().allArguments();
        // Add system properties passed as parameter
        if (systemProperties != null) {
            systemProperties.stream().map(entry -> "-D" + entry).forEach(args::add);
        }

        // Add id - it's important as it's the application mark.
        args.add("-Dserving.id=" + getId());
        return args;
    }

    private String getId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (isProcessExists(id)) {
            out.println(String.format("A konduit server with an id: '%s' already exists.", id));
            System.exit(1);
        }

        return id;
    }

    public boolean isProcessExists(String id) {
        List<String> args;

        if(SystemUtils.IS_OS_WINDOWS) {
            args = Arrays.asList("WMIC", "PROCESS", "WHERE", "\"CommandLine like '%serving.id=" + id + "' and name!='wmic.exe'\"", "GET", "CommandLine");
        } else {
            args = Arrays.asList("sh", "-c", "ps ax | grep \"Dserving.id=" + id + "$\"");
        }

        String output = "";
        try {
            Process process = new ProcessBuilder(args).start();
            output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            out.println("An error occurred while checking for existing processes:\n" + exception.getMessage());
            System.exit(1);
        }

        return output.trim()
                .replace(System.lineSeparator(), "")
                .matches("(.*)Dserving.id=" + id);
    }
}
