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

package ai.konduit.serving.util.python;

import ai.konduit.serving.executioner.PythonExecutioner;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

@javax.annotation.concurrent.NotThreadSafe
public class PythonSetupRunTest {
    @Test
    public void testPythonWithSetupAndRun() throws  Exception{
        String code = "def setup():" +
                            "global counter;counter=0\n" +
                      "def run(step):" +
                            "global counter;" +
                            "counter+=step;" +
                            "return {\"counter\":counter}";
        PythonVariables pyInputs = new PythonVariables();
        pyInputs.addInt("step", 2);
        PythonVariables pyOutputs = new PythonVariables();
        pyOutputs.addInt("counter");
        PythonExecutioner.execWithSetupAndRun(code, pyInputs, pyOutputs);
        assertEquals((long)pyOutputs.getIntValue("counter"), 2L);
        pyInputs.addInt("step", 3);
        PythonExecutioner.execWithSetupAndRun(code, pyInputs, pyOutputs);
        assertEquals((long)pyOutputs.getIntValue("counter"), 5L);
    }
}
