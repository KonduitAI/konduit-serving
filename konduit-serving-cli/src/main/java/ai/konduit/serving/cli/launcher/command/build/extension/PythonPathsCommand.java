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

package ai.konduit.serving.cli.launcher.command.build.extension;

import ai.konduit.serving.pipeline.api.python.models.CondaDetails;
import ai.konduit.serving.pipeline.api.python.models.PythonDetails;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.konduit.serving.pipeline.api.python.PythonPathUtils.findCondaInstallations;
import static ai.konduit.serving.pipeline.api.python.PythonPathUtils.findPythonInstallations;

@Name("pythonpaths")
@Summary("A utility command to manage system installed and manually registered python binaries.")
@Description("A utility command to manage system installed and manually registered python binaries. Python binaries " +
        "could be from either a regular python install, through a conda environment, javacpp cpython or a virtual " +
        "environment through 'venv' package. Each python installation has been assigned a particular id and a " +
        "specific type which ultimately identifies which python installation is going to be used with a particular " +
        "PythonStep configuration. You can also register a python binary if it's not listed through 'pythonpaths add' " +
        "subcommand. \n\n" +
        "Example usages:\n" +
        "--------------\n" +
        "- Lists all the installed and registered python binaries:\n" +
        "$ konduit pythonpaths list \n\n" +
        "- Lists python installs with their included packages:\n" +
        "$ konduit pythonpaths -wip \n\n" +
        "- Register a custom python installation:\n" +
        "$ konduit pythonpaths add -t=python -p=E:\\python37\\python.exe \n" +
        "--------------")
@Slf4j
public class PythonPathsCommand extends DefaultCommand {

    private SubCommand subCommand;
    private Object type;
    private String path;
    private boolean withInstalledPackages;

    @Argument(index = 0, argName = "sub_command", required = false)
    @DefaultValue("LIST")
    @Description("Sub command to be used with the pythonpaths command. Sub commands are: [add, list]. " +
            "Defaults to 'LIST'")
    public void setSubCommand(String subCommand) {
        try {
            this.subCommand = PythonPathsCommand.SubCommand.valueOf(subCommand.toUpperCase());
        } catch (Exception e) {
            System.out.format("Invalid sub command name: '%s'. Allowed values are: %s -> (case insensitive).",
                    subCommand, Arrays.toString(PythonPathsCommand.SubCommand.values()));
            System.exit(1);
        }
    }

    @Option(shortName = "t", longName = "type", argName = "type", required = true)
    @Description("Name of the python type. For the 'add' subcommand, accepted values are: [python, conda, venv]. " +
            "For the 'list' subcommand, accepted values are: [all, javacpp, python, conda, venv]")
    public void setType(String type) {
        this.type = type;
    }

    @Option(shortName = "p", longName = "path", argName = "install_path")
    @Description("Absolute path of the python installation. For conda and venv types this refers to the absolute path " +
            "of the root installation folder.")
    public void setPath(String path) {
        this.path = path;
    }

    @Option(shortName = "wip", longName = "with-installed-packages", flag = true)
    @Description("Absolute path of the python installation. For conda and venv types this refers to the absolute path " +
            "of the root installation folder.")
    public void setPath(boolean withInstalledPackages) {
        this.withInstalledPackages = withInstalledPackages;
    }

    private enum SubCommand {
        ADD, LIST
    }

    private enum Type {
        PYTHON, CONDA, VENV
    }

    public enum ListInstallationType {
        ALL, JAVACPP, PYTHON, CONDA, VENV
    }

    @Override
    public void run() {
        switch (this.subCommand) {
            case ADD:
                try {
                    this.type = PythonPathsCommand.Type.valueOf(((String) type).toUpperCase());
                } catch (Exception e) {
                    out.format("Invalid type name: '%s'. Allowed values are: %s -> (case insensitive).",
                            type, Arrays.toString(PythonPathsCommand.Type.values()));
                    System.exit(1);
                }
                break;
            case LIST:
                try {
                    this.type = PythonPathsCommand.ListInstallationType.valueOf(((String) type).toUpperCase());
                } catch (Exception e) {
                    out.format("Invalid type name: '%s'. Allowed values are: %s -> (case insensitive).",
                            type, Arrays.toString(PythonPathsCommand.ListInstallationType.values()));
                    System.exit(1);
                }
                break;
            default:
                out.format("Invalid sub command name: '%s'. Allowed values are: %s -> (case insensitive).",
                        subCommand, Arrays.toString(PythonPathsCommand.SubCommand.values()));
        }

        switch (subCommand) {
            case ADD:
                registerInstallation();
                break;
            case LIST:
                listInstallations((ListInstallationType) type);
                break;
            default:
                log.error("Invalid sub command name: {}. Allowed values are: {} -> (case insensitive).",
                        subCommand, Arrays.toString(PythonPathsCommand.SubCommand.values()));
        }
    }


    public static void listInstallations(ListInstallationType type) {
        switch (type) {
            case ALL:
                listJavacppInstallations();
                listPythonInstallations();
                listCondaInstallations();
                listVenvInstallations();
                break;
            case JAVACPP:
                listJavacppInstallations();
                break;
            case PYTHON:
                listPythonInstallations();
                break;
            case CONDA:
                listCondaInstallations();
                break;
            case VENV:
                listVenvInstallations();
                break;
            default:
                System.out.format("Invalid installation type name: '%s'. Allowed values are: %s -> (case insensitive).",
                        type.name(), Arrays.toString(PythonPathsCommand.ListInstallationType.values()));
        }
    }

    public static void registerInstallation() {

    }

    private static void listJavacppInstallations() {

    }

    private static void listPythonInstallations() {
        System.out.println("\n----------------------------PYTHON INSTALLS----------------------------");
        System.out.print(
                findPythonInstallations().stream()
                        .map(PythonPathsCommand::formatPythonInstallation)
                        .collect(Collectors.joining(System.lineSeparator()))
        );
        System.out.println("-----------------------------------------------------------------------");
    }


    private static void listCondaInstallations() {
        System.out.println("\n----------------------------CONDA INSTALLS-----------------------------");
        System.out.print(
                findCondaInstallations().stream()
                        .map(PythonPathsCommand::formatCondaInstallation)
                        .collect(Collectors.joining(System.lineSeparator()))
        );
        System.out.println("-----------------------------------------------------------------------");
    }

    private static String formatPythonInstallation(PythonDetails pythonDetails) {
        return formatPythonInstallation(pythonDetails, 1);
    }

    private static String formatPythonInstallation(PythonDetails pythonDetails, int numberOfTabs) {
        String tabs = IntStream.range(0, numberOfTabs).mapToObj(index -> "\t").collect(Collectors.joining(""));
        return String.format(" -%s%s: %s%n%spath: %s%n%sversion: %s",
                "\t",
                numberOfTabs > 1 ? "name" : "id",
                pythonDetails.id(),
                tabs,
                pythonDetails.path(),
                tabs,
                pythonDetails.version());
    }

    private static String formatCondaInstallation(CondaDetails condaDetails) {
        List<String> formattedCondaEnvironments = new ArrayList<>();

        condaDetails.environments().forEach(pythonDetails ->
                formattedCondaEnvironments.add(formatPythonInstallation(pythonDetails, 2)));

        return String.format(" -\tid: %s%n\tpath: %s%n\tversion: %s%s",
                condaDetails.id(),
                condaDetails.path(),
                condaDetails.version(),
                String.format(
                                "\t--------------------------ENVIRONMENTS-------------------------%n" +
                                "\t%s" +
                                "\t---------------------------------------------------------------%n",
                        String.join(System.lineSeparator() + "\t", formattedCondaEnvironments)
                )
        );
    }

    private static void listVenvInstallations() {

    }


}
