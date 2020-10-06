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

package ai.konduit.serving.pipeline.api.python;

import ai.konduit.serving.pipeline.api.process.ProcessUtils;
import ai.konduit.serving.pipeline.api.python.models.*;
import ai.konduit.serving.pipeline.settings.DirectoryFetcher;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.shade.guava.collect.Streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PythonPathUtils {

    public static final String FINDER_COMMAND = ProcessUtils.isWindows() || ProcessUtils.isMac() ? "where" : "which";
    private static final File registeredInstallDetailsLocation = new File(DirectoryFetcher.getProfilesDir(), "registered-installs.json");

    public static List<PythonDetails> findPythonInstallations() {
        List<String> pythonInstallationPaths = findInstallationPaths(PythonType.PYTHON.name().toLowerCase());
        List<String> allPythonInstallations = Streams.concat(pythonInstallationPaths.stream(),
                filterRegisteredInstalls(PythonType.PYTHON).stream().map(RegisteredPythonInstall::path)).collect(Collectors.toList());

        return IntStream.range(0, allPythonInstallations.size())
                .mapToObj(index -> getPythonDetails(String.valueOf(index + 1), allPythonInstallations.get(index)))
                .collect(Collectors.toList());
    }

    public static PythonDetails getPythonDetails(String id, String pythonPath) {
        return new PythonDetails(id, pythonPath, getPythonVersion(pythonPath));
    }

    public static List<CondaDetails> findCondaInstallations() {
        List<String> condaInstallationPaths = findInstallationPaths(PythonType.CONDA.name().toLowerCase());
        List<String> allCondaInstalls = Streams.concat(condaInstallationPaths.stream(),
                filterRegisteredInstalls(PythonType.CONDA).stream().map(RegisteredPythonInstall::path)).collect(Collectors.toList());

        return IntStream.range(0, allCondaInstalls.size())
                .mapToObj(index -> getCondaDetails(String.valueOf(index + 1), allCondaInstalls.get(index)))
                .collect(Collectors.toList());
    }

    public static CondaDetails getCondaDetails(String id, String condaPath) {
        return new CondaDetails(id, condaPath, getCondaVersion(condaPath), findCondaEnvironments(condaPath));
    }

    public static List<VenvDetails> findVenvInstallations() {
        List<RegisteredPythonInstall> allVenvInstallations = filterRegisteredInstalls(PythonType.VENV);

        return IntStream.range(0, allVenvInstallations.size())
                .mapToObj(index -> getPythonDetails(String.valueOf(index + 1), getVenvPythonFile(allVenvInstallations.get(index).path()).getAbsolutePath()))
                .map(pythonDetails -> new VenvDetails(pythonDetails.id(), pythonDetails.path(), pythonDetails.version()))
                .collect(Collectors.toList());
    }

    public static String getPythonVersion(String pythonPath) {
        String pythonCommandOutput = ProcessUtils.runAndGetOutput(pythonPath, "-c", "import sys; print('Python ' + sys.version.split(' ')[0])");
        if(pythonCommandOutput.contains("Python ")) {
            return pythonCommandOutput.replace("Python ", "");
        } else {
            throw new IllegalStateException(String.format("Path at '%s' is not a valid python executable.", pythonPath));
        }
    }

    public static String getCondaVersion(String condaPath) {
        String condaCommandOutput = ProcessUtils.runAndGetOutput(condaPath, "--version");
        if(condaCommandOutput.contains("conda ")) {
            return condaCommandOutput.replace("conda ", "");
        } else {
            throw new IllegalStateException(String.format("Path at '%s' is not a valid conda executable.", condaPath));
        }
    }

    public static String getVenvVersion(String venvPath) {
        File venvPythonPath = getVenvPythonFile(venvPath);
        if(!venvPythonPath.exists()) {
            throw new IllegalStateException(String.format("Unable to find python path associated with the virtual environment at '%s'. " +
                    "Please ensure the specified venv path at '%s' is a valid python virtual environment.", venvPythonPath.getAbsoluteFile(), new File(venvPath).getAbsoluteFile()));
        } else {
            return getPythonVersion(venvPythonPath.getAbsolutePath());
        }
    }

    public static File getVenvPythonFile(String venvPath) {
        return Paths.get(venvPath, ProcessUtils.isWindows() ? "Scripts" : "bin", ProcessUtils.isWindows() ? "python.exe" : "python").toFile();
    }

    public static String getPythonPathFromRoot(String rootPath) {
        return (ProcessUtils.isWindows() ?
                Paths.get(rootPath, "python.exe") :
                Paths.get(rootPath, "bin", "python"))
                                .toFile().getAbsolutePath();
    }

    public static List<String> findInstallationPaths(String type) {
        return Arrays.stream(ProcessUtils.runAndGetOutput(FINDER_COMMAND, type).split(System.lineSeparator()))
                .collect(Collectors.toList());
    }

    public static List<PythonDetails> findCondaEnvironments(String condaPath) {
        return Arrays.stream(
                ProcessUtils.runAndGetOutput(condaPath, "info", "-e")
                        .replace("*", " ")
                        .replace("# conda environments:", "")
                        .replace("#", "")
                        .trim()
                        .split(System.lineSeparator()))
                .collect(Collectors.toList())
                .stream()
                .map(envInfo -> {
                    String[] envInfoSplits = envInfo.split("\\s+", 2);
                    String resolvedPythonPath = getPythonPathFromRoot(envInfoSplits[1]);
                    return new PythonDetails(envInfoSplits[0], resolvedPythonPath, getPythonVersion(resolvedPythonPath));
                }).collect(Collectors.toList());
    }

    public static void registerInstallation(PythonType pythonType, String path) {
        List<RegisteredPythonInstall> registeredPythonInstalls = getRegisteredPythonInstalls();
        path = new File(path).getAbsolutePath();

        switch (pythonType) {
            case PYTHON:
                String pythonVersion = getPythonVersion(path);
                registeredPythonInstalls.add(new RegisteredPythonInstall(pythonType, path, pythonVersion));
                break;
            case CONDA:
                String condaVersion = getCondaVersion(path);
                registeredPythonInstalls.add(new RegisteredPythonInstall(pythonType, path, condaVersion));
                break;
            case VENV:
                String venvVersion = getVenvVersion(path);
                registeredPythonInstalls.add(new RegisteredPythonInstall(pythonType, path, venvVersion));
                break;
        }

        saveRegisteredPythonInstalls(registeredPythonInstalls);
        System.out.format("Registered installation of type: '%s' from location: '%s'%n", pythonType.name(), path);
    }

    public static List<RegisteredPythonInstall> getRegisteredPythonInstalls() {
        try {
            return ObjectMappers.fromJson(FileUtils.readFileToString(registeredInstallDetailsLocation, StandardCharsets.UTF_8), RegisteredPythonInstalls.class).registeredPythonInstalls();
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
            return new ArrayList<>();
        }
    }

    public static List<RegisteredPythonInstall> filterRegisteredInstalls(PythonType pythonType) {
        return getRegisteredPythonInstalls().stream()
                .filter(registeredPythonInstall -> registeredPythonInstall.pythonType().equals(pythonType))
                .collect(Collectors.toList());
    }

    public static void saveRegisteredPythonInstalls(List<RegisteredPythonInstall> registeredPythonInstalls) {
        try {
            FileUtils.writeStringToFile(registeredInstallDetailsLocation, ObjectMappers.toJson(new RegisteredPythonInstalls(registeredPythonInstalls)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public enum PythonType {
        PYTHON, CONDA, VENV
    }
}
