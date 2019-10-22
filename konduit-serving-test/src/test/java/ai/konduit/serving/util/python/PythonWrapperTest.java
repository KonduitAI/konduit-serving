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

import ai.konduit.serving.PythonWrapper.Python;
import ai.konduit.serving.PythonWrapper.PythonObject;
import ai.konduit.serving.executioner.PythonExecutioner;
import lombok.var;
import org.junit.Assert;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;

import static org.junit.Assert.*;

@javax.annotation.concurrent.NotThreadSafe
public class PythonWrapperTest {
    @Test
    public void testPythonWrapperBasics(){
        PythonExecutioner.init();
        PythonExecutioner.acquireGIL();
        var list = new PythonObject(new ArrayList());
        list.attr("append").call("item1");
        list.attr("append").call("item2");
        String str = Python.str(list);
        assertEquals(str, "[\'item1\', \'item2\']");
        PythonExecutioner.releaseGIL();
    }


    @Test
    public void testPythonWrapperWithNumpy(){
        PythonExecutioner.init();
        var np = Python.importModule("numpy");
        var pyZeros = np.attr("zeros").call(Arrays.asList(Arrays.asList(32, 16)));
        INDArray zeros = pyZeros.toNumpy().getNd4jArray();
        assertArrayEquals(zeros.shape(), new long[]{32, 16});

    }

    //@Test
    public void testPythonWrappersWithTF(){
        PythonExecutioner.init();
        // import stuff
        var tf = Python.importModule("tensorflow");
        var keras = tf.attr("keras");

        // build model
        var model = keras.attr("models").attr("Sequential").call();
        System.out.println("---ok----");
        Map denseArgs = new HashMap();
        denseArgs.put("units", 10);
        denseArgs.put("input_dim", 5);
        var denseLayer = keras.attr("layers").attr("Dense").call(denseArgs);
        model.attr("add").call(denseLayer);

        //prediction
        INDArray input = Nd4j.rand(12, 5);
        INDArray output =  model.attr("predict").call(input).toNumpy().getNd4jArray();

        assertArrayEquals(output.shape(), new long[]{12, 10});
        PythonExecutioner.releaseGIL();

    }


}
