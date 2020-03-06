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

package ai.konduit.serving.codegen.buildscripts;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.util.Arrays;
import java.util.List;

public class BuildScriptGen {

    @Parameter(names = {"--chip"}, validateValueWith = ChipValidator.class)
    private String chip = "cpu";

    @Parameter(names = "--os", description = "os", validateValueWith = OsValidator.class, required = true)
    private String os;

    @Parameter(names = "--usePython", description = "whether to use python or not")
    private boolean usePython = true;

    @Parameter(names = "--usePmml", description = "whether to use pmml (note: pmml is agpl licensed) or not")
    private boolean usePmml = true;

    public static void main(String... args) throws Exception {
        new BuildScriptGen().runMain(args);
    }

    public void runMain(String... args) throws Exception {
        JCommander jCommander = new JCommander(this);
        jCommander.parse(args);


        StringBuffer command = new StringBuffer();
        command.append("mvn clean install -Dmaven.test.skip=true");
        String[] oses = {
                "windows-x86_64",
                "linux-x86_64",
                "linux-x86_64-gpu",
                "macosx-x86_64",
                "linux-armhf",
                "windows-x86_64-gpu"
        };

        for (String os : oses) {
            StringBuffer buildCommandForOs = new StringBuffer();
            buildCommandForOs.append(command.toString());
            buildCommandForOs.append(" -Djavacpp.platform=" + os + " ");
            if (!os.contains("arm") && !os.contains("gpu")) {
                buildCommandForOs.append(" -Dchip=cpu -Ppython");
            } else if (os.contains("arm")) {
                buildCommandForOs.append("-Dchip=arm");
            } else if (os.contains("gpu")) {
                buildCommandForOs.append(" -Dchip=gpu");
            }

            if (usePython) {
                buildCommandForOs.append(" -Ppython");
            }

            if (usePmml) {
                buildCommandForOs.append(" -Ppmml");
            }


            Runtime run = Runtime.getRuntime();
            Process proc = run.exec(buildCommandForOs.toString());
            int wait = proc.waitFor();
            if (wait != 0) {
                throw new IllegalStateException("Program didn't finish successfully. Please run " + buildCommandForOs.toString());
            }
        }

    }

    public static class ChipValidator implements IValueValidator<String> {
        private List<String> chips = Arrays.asList(
                "gpu", "cpu", "arm"
        );


        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!chips.contains(value)) {
                throw new ParameterException("Os value must be one of " + chips);
            }

        }
    }

    public static class OsValidator implements IValueValidator<String> {
        private List<String> oses = Arrays.asList(
                "windows-x86_64",
                "linux-x86_64",
                "macosx-x86_64",
                "linux-armhf",
                "linux-arm64",
                "windows-x86_64-gpu"
        );


        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!oses.contains(value)) {
                throw new ParameterException("Os value must be one of " + oses);
            }

        }
    }

}
