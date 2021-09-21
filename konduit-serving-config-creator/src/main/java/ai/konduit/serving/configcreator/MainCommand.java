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
package ai.konduit.serving.configcreator;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.Writer;

@CommandLine.Command(name = "konduit",subcommands = {
        InferenceServerCreate.class,
        SequencePipelineCombiner.class,
        StepCreator.class
},mixinStandardHelpOptions = true)
public class MainCommand {

    /**
     * Create a command line initializing {@link StepCreator}
     * dynamic {@link picocli.CommandLine.Model.CommandSpec}
     * with System.out as the default initializer
     * @return the associated {@link CommandLine}
     * @throws Exception
     */
    public static CommandLine createCommandLine() throws Exception {
        return createCommandLine(null);
    }

    /**
     * Create a {@link CommandLine}
     * with a dynamic {@link StepCreator}
     * {@link picocli.CommandLine.Model.CommandSpec}
     * and an optional (can be null) {@link PrintWriter}
     * for collecting output
     * @param out
     * @return
     * @throws Exception
     */
    public static CommandLine createCommandLine(Writer out) throws Exception {
        CommandLine commandLine = new CommandLine(new MainCommand());
        if(out != null) {
            commandLine.setOut(new PrintWriter(out));
        }



        return commandLine;
    }

        public static void main(String...args) throws Exception {
        CommandLine commandLine = MainCommand.createCommandLine();
        int exit = commandLine.execute(args);
        System.exit(exit);
    }

}
