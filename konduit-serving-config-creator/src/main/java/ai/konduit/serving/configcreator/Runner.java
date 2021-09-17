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

import ai.konduit.serving.cli.launcher.KonduitServingLauncher;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "run",description = "Run a command related to executing models.")
public class Runner implements Runnable {
    @CommandLine.Option(names = "-exec", parameterConsumer = ExecParameterConsumer.class)
    List<String> list = new ArrayList<>();
    @CommandLine.Unmatched
    List<String> unmatched = new ArrayList<>();

    @Override
    public void run() {
        if(!list.isEmpty())
            new KonduitServingLauncher().exec(list.toArray(new String[list.size()]));
        else
            //unmatched arguments make -exec optional
            new KonduitServingLauncher().exec(unmatched.toArray(new String[unmatched.size()]));

    }

}
