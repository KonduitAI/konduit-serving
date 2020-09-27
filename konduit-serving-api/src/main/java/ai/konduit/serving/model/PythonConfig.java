/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.model;

import ai.konduit.serving.pipeline.api.TextConfig;
import ai.konduit.serving.pipeline.api.process.ProcessUtils;
import ai.konduit.serving.pipeline.api.python.PythonPathUtils;
import ai.konduit.serving.pipeline.api.python.models.CondaDetails;
import ai.konduit.serving.pipeline.api.python.models.PythonDetails;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Python configuration for specifying:
 *   1. pythonCode: actual python source code
 *   2. pythonCodePath: a path to a source file.
 *   3. pythonPath: a python path for dependencies
 *   4. pythonInputs/pythonOutputs/extraInputs: a map of variable name to python type
 *   5. returnAllInputs: rather than specifying outputs explicitly, the python execution
 *   will just return all created python variables during execution
 *
 *
 * @author Adam Gibson
 */
@Data
@NoArgsConstructor
@Slf4j
@Builder
@AllArgsConstructor
public class PythonConfig implements Serializable, TextConfig {

    private PythonType pythonType;
    private String pythonPath;
    private String environmentName;
    private AppendType appendType;

    private String pythonCode, pythonCodePath, pythonLibrariesPath, importCode, importCodePath;

    @Singular
    @Deprecated
    private Map<String, String> pythonInputs, pythonOutputs, extraInputs;

    private boolean returnAllInputs,setupAndRun;

    @Singular("ioInput")
    private Map<String,PythonIO> ioInputs;

    @Singular("ioOutput")
    private Map<String,PythonIO> ioOutputs;

    @Builder.Default
    private String jobSuffix = "konduit_job";

    public String resolvePythonLibrariesPath() {
        switch (pythonType) {
            case JAVACPP:
                break;
            case PYTHON:
                this.pythonLibrariesPath = findPythonLibariesPath(pythonPath);
                break;
            case CONDA:
                this.pythonLibrariesPath = pythonLibrariesFromAbsolutePath(PythonPathUtils.findCondaInstallations().stream()
                        .filter(condaDetails -> condaDetails.id().equals(pythonPath)).findFirst().get().environments().stream()
                        .filter(pythonDetails -> pythonDetails.id().equals(environmentName)).findFirst().get().path());
                break;
            case VENV:
                break;
            case CUSTOM:
                this.pythonLibrariesPath = pythonLibrariesFromAbsolutePath(pythonPath);
                break;
            default:
                break;
        }

        return this.pythonLibrariesPath;
    }

    public static String findPythonLibrariesPathFromCondaDetails(String condaPathId, String environmentName) {
        CondaDetails condaDetails = findCondaDetails(condaPathId);

        List<PythonDetails> pythonDetailsList = condaDetails.environments();
        Optional<PythonDetails> optionalPythonDetails = pythonDetailsList
                .stream()
                .filter(pythonDetails -> pythonDetails.id().equals(environmentName))
                .findFirst();

        if(optionalPythonDetails.isPresent()) {
            return pythonLibrariesFromAbsolutePath(optionalPythonDetails.get().path());
        } else {
            throw new IllegalStateException(String.format("No environment available with the name '%s' for conda path id '%s'. Available python environments for conda path id '%s' are: %s",
                    environmentName, condaPathId, condaPathId,
                    String.format("%n---%n%s%n---%n", pythonDetailsList.stream()
                            .map(pythonDetails -> String.format("-\tname: %s%n\tpath: %s%n\tversion: %s%n",
                                    pythonDetails.id(), pythonDetails.path(), pythonDetails.version()))
                            .collect(Collectors.joining(System.lineSeparator()))
                    )));
        }
    }

    public static CondaDetails findCondaDetails(String condaPathId) {
        List<CondaDetails> condaDetailsList = PythonPathUtils.findCondaInstallations();
        Optional<CondaDetails> optionalCondaDetails = condaDetailsList
                .stream()
                .filter(condaDetails -> condaDetails.id().equals(condaPathId))
                .findFirst();

        if(optionalCondaDetails.isPresent()) {
            return optionalCondaDetails.get();
        } else {
            throw new IllegalStateException(String.format("No id '%s' available for conda path type. Available conda type paths are: %s",
                    condaPathId,
                    String.format("%n---%n%s%n---%n", condaDetailsList.stream()
                            .map(condaDetails -> String.format("-\tid: %s%n\tpath: %s%n\tversion: %s%n",
                                    condaDetails.id(), condaDetails.path(), condaDetails.version()))
                            .collect(Collectors.joining(System.lineSeparator()))
                    )));
        }
    }

    public static String findPythonLibariesPath(String pythonPathId) {
        List<PythonDetails> pythonDetailsList = PythonPathUtils.findPythonInstallations();
        Optional<PythonDetails> optionalPythonDetails = pythonDetailsList
                .stream()
                .filter(pythonDetails -> pythonDetails.id().equals(pythonPathId))
                .findFirst();

        if(optionalPythonDetails.isPresent()) {
            return pythonLibrariesFromAbsolutePath(optionalPythonDetails.get().path());
        } else {
            throw new IllegalStateException(String.format("No id '%s' available for python path type. Available python type paths are: %s",
                    pythonPathId,
                    String.format("%n---%n%s%n---%n", pythonDetailsList.stream()
                            .map(pythonDetails -> String.format("-\tid: %s%n\tpath: %s%n\tversion: %s%n",
                                    pythonDetails.id(), pythonDetails.path(), pythonDetails.version()))
                            .collect(Collectors.joining(System.lineSeparator()))
                    )));
        }
    }

    public static String pythonLibrariesFromAbsolutePath(String pythonPath) {
        File pythonPathFile = new File(pythonPath);
        if(pythonPathFile.exists() && pythonPathFile.isFile()) {
            return ProcessUtils.runAndGetOutput(pythonPath, "-c", "import sys, os; print(os.pathsep.join([path for path in sys.path if path]))").replace(System.lineSeparator(), "").trim();
        } else {
            throw new IllegalStateException(String.format("No python executable path exist at: '%s'", pythonPathFile.getAbsoluteFile()));
        }
    }

    public enum PythonType {
        JAVACPP,
        PYTHON,
        CONDA,
        VENV,
        CUSTOM
    }

    public enum AppendType {
        BEFORE,
        NONE,
        AFTER
    }
}
