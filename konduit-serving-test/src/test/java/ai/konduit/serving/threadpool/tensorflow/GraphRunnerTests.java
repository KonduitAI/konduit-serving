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

import org.nd4j.tensorflow.conversion.TensorDataType;
import org.nd4j.tensorflow.conversion.graphrunner.GraphRunner;
import org.nd4j.tensorflow.conversion.TensorflowConversion;
import org.bytedeco.tensorflow.TF_Tensor;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static junit.framework.TestCase.assertEquals;

public class GraphRunnerTests {

    @Test()
    public void testGraphRunnerCast() {
        INDArray arr = Nd4j.linspace(1, 4, 4).castTo(DataType.FLOAT);
        TF_Tensor tensor = TensorflowConversion.getInstance().tensorFromNDArray(arr);
        TF_Tensor tf_tensor = GraphRunner.castTensor(tensor, TensorDataType.FLOAT, TensorDataType.DOUBLE);
        INDArray doubleNDArray = TensorflowConversion.getInstance().ndArrayFromTensor(tf_tensor);
        assertEquals(DataType.DOUBLE, doubleNDArray.dataType());

        arr = arr.castTo(DataType.INT);
        tensor = TensorflowConversion.getInstance().tensorFromNDArray(arr);
        tf_tensor = GraphRunner.castTensor(tensor, TensorDataType.fromNd4jType(DataType.INT), TensorDataType.DOUBLE);
        doubleNDArray = TensorflowConversion.getInstance().ndArrayFromTensor(tf_tensor);
        assertEquals(DataType.DOUBLE, doubleNDArray.dataType());

    }

}
