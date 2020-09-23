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

import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

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

    @Option(shortName = "t", longName = "type", argName = "python_type")
    @DefaultValue("PYTHON")
    @Description("Name of the python type. For the 'add' subcommand, accepted values are: [python, conda, venv]. " +
            "For the 'list' subcommand, accepted values are: [all, javacpp, python, conda, venv]")
    public void setType(String type) {
        switch (this.subCommand) {
            case ADD:
                try {
                    this.type = PythonPathsCommand.Type.valueOf(type.toUpperCase());
                } catch (Exception e) {
                    System.out.format("Invalid type name: '%s'. Allowed values are: %s -> (case insensitive).",
                            subCommand, Arrays.toString(PythonPathsCommand.Type.values()));
                    System.exit(1);
                }
                break;
            case LIST:
                try {
                    this.type = PythonPathsCommand.ListInstallationType.valueOf(type.toUpperCase());
                } catch (Exception e) {
                    System.out.format("Invalid type name: '%s'. Allowed values are: %s -> (case insensitive).",
                            subCommand, Arrays.toString(PythonPathsCommand.ListInstallationType.values()));
                    System.exit(1);
                }
                break;
            default:
                System.out.format("Invalid sub command name: '%s'. Allowed values are: %s -> (case insensitive).",
                        subCommand, Arrays.toString(PythonPathsCommand.SubCommand.values()));
        }
    }

    @Option(shortName = "p", longName = "path", argName = "install_path")
    @DefaultValue("PYTHON")
    @Description("Absolute path of the python installation. For conda and venv types this refers to the absolute path " +
            "of the root installation folder.")
    public void setPath(String path) {
        this.path = path;
    }

    @Option(shortName = "wip", longName = "with-installed-packages", flag = true)
    @DefaultValue("PYTHON")
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

    private static void listVenvInstallations() {

    }

    private static void listCondaInstallations() {

    }

    private static void listPythonInstallations() {

    }

    private static void listJavacppInstallations() {

    }

    public static void registerInstallation() {

    }
}
