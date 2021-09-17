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

import java.util.List;
import java.util.Stack;

public class ExecParameterConsumer implements CommandLine.IParameterConsumer {
    public ExecParameterConsumer() {
    }


    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        List<String> list = argSpec.getValue();
        while (!args.isEmpty()) {
            String arg = args.pop();
            list.add(arg);
        }

        //remove the help prompt and replace it with our underlying help function.
        if(list.size() == 1 && list.contains("--help") || list.contains("-h")) {
            list.clear();
        }

        //add this as default for the user to show a proper help command
        if(list.isEmpty()) {
            list.add("-exec");
        }

        //also always ensure that if the user omits exec, specify it as the first parameter
        //allowing seamless bridging
        if(!list.isEmpty() && !list.get(0).equals("-exec")) {
            list.add(0,"-exec");
        }


    }
}
