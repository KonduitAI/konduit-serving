/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package ai.konduit.serving.configcreator.integrationtest;

import ai.konduit.serving.configcreator.MainCommand;
import picocli.CommandLine;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;


public class CLITestCase {

    private String command;
    private CommandLine commandLine;
    private Writer stringWriter;

    public CLITestCase(String command) {
        stringWriter = new StringWriter();
        this.command = command;

        try {
            commandLine = MainCommand.createCommandLine(stringWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exec() {
        exec(0);
    }

    public void exec(int expectedStatusCode) {
        int execute = commandLine.execute(command.split(" "));
        assertEquals("Command " + command + " failed to execute with status " + execute,expectedStatusCode,execute);

    }

    public void executeAndAssertSuccess(String assertionOutput) {
        int execute = commandLine.execute(command.split(" "));
        assertEquals("Command " + command + " failed to execute with status " + execute,0,execute);
        assertEquals(assertionOutput,stringWriter.toString());
    }

    public void close() throws IOException {
        stringWriter.close();
    }



    public String getCommand() {
        return command;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public Writer getStringWriter() {
        return stringWriter;
    }
}
