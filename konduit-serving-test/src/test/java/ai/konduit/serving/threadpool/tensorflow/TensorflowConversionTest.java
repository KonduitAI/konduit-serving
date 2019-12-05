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

package ai.konduit.serving.threadpool.tensorflow;

import org.bytedeco.tensorflow.TF_Tensor;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.tensorflow.conversion.TensorflowConversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TensorflowConversionTest {

    @Test(timeout = 60000)

    public void testView() {
        INDArray matrix = Nd4j.linspace(1, 8, 8).reshape(2, 4);
        INDArray view = matrix.slice(0);
        TensorflowConversion conversion = TensorflowConversion.getInstance();
        TF_Tensor tf_tensor = conversion.tensorFromNDArray(view);
        INDArray converted = conversion.ndArrayFromTensor(tf_tensor);
        assertEquals(view, converted);
    }

    @Test(timeout = 60000, expected = IllegalArgumentException.class)
    public void testNullArray() {
        INDArray array = Nd4j.create(2, 2);
        array.setData(null);
        TensorflowConversion conversion = TensorflowConversion.getInstance();
        TF_Tensor tf_tensor = conversion.tensorFromNDArray(array);
        fail();
    }

    @Test(timeout = 60000)

    public void testConversionFromNdArray() throws Exception {
        INDArray arr = Nd4j.linspace(1, 4, 4);
        TensorflowConversion tensorflowConversion = TensorflowConversion.getInstance();
        TF_Tensor tf_tensor = tensorflowConversion.tensorFromNDArray(arr);
        INDArray fromTensor = tensorflowConversion.ndArrayFromTensor(tf_tensor);
        assertEquals(arr, fromTensor);
        arr.addi(1.0);
        tf_tensor = tensorflowConversion.tensorFromNDArray(arr);
        fromTensor = tensorflowConversion.ndArrayFromTensor(tf_tensor);
        assertEquals(arr, fromTensor);


    }


    @Test(timeout = 60000)

    public void testStringConversion() throws Exception {
        String[] strings = {"one", "two", "three"};
        INDArray arr = Nd4j.create(strings);
        TensorflowConversion tensorflowConversion = TensorflowConversion.getInstance();
        TF_Tensor tf_tensor = tensorflowConversion.tensorFromNDArray(arr);
        INDArray fromTensor = tensorflowConversion.ndArrayFromTensor(tf_tensor);
        assertEquals(arr.length(), fromTensor.length());
        for (int i = 0; i < arr.length(); i++) {
            assertEquals(strings[i], fromTensor.getString(i));
            assertEquals(arr.getString(i), fromTensor.getString(i));
        }
    }

}
