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
import ai.konduit.serving.pipeline.api.python.models.CondaDetails;
import ai.konduit.serving.pipeline.api.python.models.PythonDetails;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class PythonPathUtils {

    public static String FINDER_COMMAND = ProcessUtils.isWindows() || ProcessUtils.isMac() ? "where" : "which";

    public static List<PythonDetails> findPythonInstallations() {
        List<String> pythonInstallationPaths = findInstallationPaths("python");

        return IntStream.range(0, pythonInstallationPaths.size())
                .mapToObj(index -> getPythonDetails(String.valueOf(index + 1), pythonInstallationPaths.get(index)))
                .collect(Collectors.toList());
    }

    public static PythonDetails getPythonDetails(String id, String pythonPath) {
        return new PythonDetails(id, pythonPath, getPythonVersion(pythonPath));
    }

    public static List<CondaDetails> findCondaInstallations() {
        List<String> condaInstallationPaths = findInstallationPaths("conda");

        return IntStream.range(0, condaInstallationPaths.size())
                .mapToObj(index -> getCondaDetails(String.valueOf(index + 1), condaInstallationPaths.get(index)))
                .collect(Collectors.toList());
    }

    public static CondaDetails getCondaDetails(String id, String condaPath) {
        return new CondaDetails(id, condaPath, getCondaVersion(condaPath), findCondaEnvironments(condaPath));
    }

    public static String getPythonVersion(String pythonPath) {
        return ProcessUtils.runAndGetOutput(pythonPath, "-c", "import sys; print(sys.version.split(' ')[0])").replace("Python ", "");
    }

    public static String getCondaVersion(String condaPath) {
        return ProcessUtils.runAndGetOutput(condaPath, "--version").replace("conda ", "");
    }

    public static String getPythonPathFromRoot(String rootPath) {
        return (ProcessUtils.isWindows() ?
                Paths.get(rootPath, "python") :
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
}
