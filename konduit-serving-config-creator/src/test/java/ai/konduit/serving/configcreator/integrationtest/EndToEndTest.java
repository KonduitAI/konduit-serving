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

import ai.konduit.serving.configcreator.StepCreator;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

public class EndToEndTest {


    @Test
    public void testHelp() {
        CLITestCase cliTestCase = new CLITestCase("-h");
        cliTestCase.exec(2);
    }

    @Test
    public void testIntegration() {
        CLITestCase cliTestCase = new CLITestCase("step-create python --fileFormat=json --pythonConfig=\"pythonConfigType=JAVACPP,appendType=NONE\"");
        cliTestCase.exec(0);
        System.out.println(cliTestCase.getStringWriter().toString());
    }


}
