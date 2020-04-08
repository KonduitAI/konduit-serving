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

package ai.konduit.serving.launcher;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class KonduitServingLauncherTest {

    private static final int TIMEOUT = 60000;

    // Needed tests
    // 2. konduit serve, -b and not -b
    // 3. konduit list, with servers, without servers
    // 4. konduit logs, -f and not -f, with servers, without servers
    // 5. konduit predict, -it IMAGE, -it JSON, -pt RAW, -pt CLASSIFICATION, with servers, without servers
    // 6. konduit stop, run servers, without servers
    // 7. konduit version, --version, version

    @Test(timeout = TIMEOUT)
    public void testMainHelp() throws IOException {
        String output = runAndGetOutput("--help"); // Testing with '--help'
        assertTrue(hasAll(output, getMainCommandNames().toArray(new String[0])));
        assertTrue(output.contains("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));

        output = runAndGetOutput("help"); // Testing with 'help'
        assertTrue(hasAll(output, getMainCommandNames().toArray(new String[0])));
        assertTrue(output.contains("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));

        output = runAndGetOutput("-h"); // Testing with '-h'
        assertTrue(hasAll(output, getMainCommandNames().toArray(new String[0])));
        assertTrue(output.contains("Usage: konduit [COMMAND] [OPTIONS] [arg...]"));
    }

    private String runAndGetOutput(String... commands) throws IOException {
        return IOUtils.toString(runAndGetInputStream(commands), StandardCharsets.UTF_8);
    }

    private InputStream runAndGetInputStream(String... commands) throws IOException {
        return new ProcessBuilder(getCommand(commands)).start().getInputStream();
    }
    private List<String> getCommand(String... commands) {
        List<String> baseCommand = new ArrayList<>(Arrays.asList("java", "-cp", System.getProperty("java.class.path"),
                "-Dvertx.cli.usage.prefix=konduit", KonduitServingLauncher.class.getCanonicalName()));
        Collections.addAll(baseCommand, commands);

        return baseCommand;
    }

    private boolean hasAll(String mainInput, String... toCompareWith) {
        return Stream.of(toCompareWith).allMatch(mainInput::contains);
    }

    private Collection<String> getMainCommandNames() {
        return new KonduitServingLauncher().setMainCommands().getCommandNames();
    }
}
