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
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertEquals;

@javax.annotation.concurrent.NotThreadSafe
public class PythonExecutionerBasicTest {


    @org.junit.Test
    public void testPythonSysVersion() {
        PythonExecutioner.exec("import sys; print(sys.version)");
    }

    @Test
    public void testStr() throws Exception {

        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addStr("x", "Hello");
        pyInputs.addStr("y", "World");

        pyOutputs.addStr("z");

        String code = "z = x + ' ' + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);

        String z = pyOutputs.getStrValue("z");

        assertEquals("Hello World", z);
    }

    @Test
    public void testInt() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addInt("x", 10);
        pyInputs.addInt("y", 20);

        String code = "z = x + y";

        pyOutputs.addInt("z");


        PythonExecutioner.exec(code, pyInputs, pyOutputs);

        long z = pyOutputs.getIntValue("z");

        Assert.assertEquals(30, z);

    }

    @Test
    public void testList() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        Object[] x = new Object[]{1L, 2L, 3L, "a", "b", "c"};
        Object[] y = new Object[]{4L, 5L, 6L, "d", "e", "f"};

        pyInputs.addList("x", x);
        pyInputs.addList("y", y);

        String code = "z = x + y";

        pyOutputs.addList("z");


        PythonExecutioner.exec(code, pyInputs, pyOutputs);

        Object[] z = pyOutputs.getListValue("z");

        Assert.assertEquals(z.length, x.length + y.length);

        for (int i = 0; i < x.length; i++) {
            if (x[i] instanceof Number) {
                Number xNum = (Number) x[i];
                Number zNum = (Number) z[i];
                Assert.assertEquals(xNum.intValue(), zNum.intValue());
            } else {
                Assert.assertEquals(x[i], z[i]);
            }

        }
        for (int i = 0; i < y.length; i++) {
            if (y[i] instanceof Number) {
                Number yNum = (Number) y[i];
                Number zNum = (Number) z[x.length + i];
                Assert.assertEquals(yNum.intValue(), zNum.intValue());
            } else {
                Assert.assertEquals(y[i], z[x.length + i]);

            }

        }

    }

    @Test
    public void testNDArrayFloat() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addNDArray("x", Nd4j.zeros(DataType.FLOAT, 2, 3));
        pyInputs.addNDArray("y", Nd4j.ones(DataType.FLOAT, 2, 3));
        pyOutputs.addNDArray("z");

        String code = "z = x + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);
        INDArray z = pyOutputs.getNDArrayValue("z").getNd4jArray();

        Assert.assertEquals(6.0, z.sum().getDouble(0), 1e-5);


    }

    @Test
    public void testTensorflowCustomAnaconda() {
        PythonExecutioner.exec("import tensorflow as tf");
    }

    @Test
    public void testNDArrayDouble() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addNDArray("x", Nd4j.zeros(DataType.DOUBLE, 2, 3));
        pyInputs.addNDArray("y", Nd4j.ones(DataType.DOUBLE, 2, 3));
        pyOutputs.addNDArray("z");

        String code = "z = x + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);
        INDArray z = pyOutputs.getNDArrayValue("z").getNd4jArray();

        Assert.assertEquals(6.0, z.sum().getDouble(0), 1e-5);
    }

    @Test
    public void testNDArrayShort() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addNDArray("x", Nd4j.zeros(DataType.SHORT, 2, 3));
        pyInputs.addNDArray("y", Nd4j.ones(DataType.SHORT, 2, 3));
        pyOutputs.addNDArray("z");

        String code = "z = x + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);
        INDArray z = pyOutputs.getNDArrayValue("z").getNd4jArray();

        Assert.assertEquals(6.0, z.sum().getDouble(0), 1e-5);
    }


    @Test
    public void testNDArrayInt() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addNDArray("x", Nd4j.zeros(DataType.INT, 2, 3));
        pyInputs.addNDArray("y", Nd4j.ones(DataType.INT, 2, 3));
        pyOutputs.addNDArray("z");

        String code = "z = x + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);
        INDArray z = pyOutputs.getNDArrayValue("z").getNd4jArray();

        Assert.assertEquals(6.0, z.sum().getDouble(0), 1e-5);

    }

    @Test
    public void testNDArrayLong() throws Exception {
        PythonVariables pyInputs = new PythonVariables();
        PythonVariables pyOutputs = new PythonVariables();

        pyInputs.addNDArray("x", Nd4j.zeros(DataType.LONG, 2, 3));
        pyInputs.addNDArray("y", Nd4j.ones(DataType.LONG, 2, 3));
        pyOutputs.addNDArray("z");

        String code = "z = x + y";

        PythonExecutioner.exec(code, pyInputs, pyOutputs);
        INDArray z = pyOutputs.getNDArrayValue("z").getNd4jArray();

        Assert.assertEquals(6.0, z.sum().getDouble(0), 1e-5);


    }

}
