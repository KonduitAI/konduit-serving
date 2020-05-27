/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.models.tensorflow;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.NDArrayType;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.tensorflow.Tensor;

import static org.junit.Assert.assertEquals;

public class TestArrayConversion {

    @Test
    public void testConversion() throws Exception {

        for(NDArrayType t : new NDArrayType[]{
                NDArrayType.DOUBLE,
                NDArrayType.FLOAT,
                NDArrayType.INT32, NDArrayType.INT64, NDArrayType.UINT8,
//                NDArrayType.BOOL      //TODO some ND4J issue
        }){

            System.out.println("----- " + t + " -----");

            INDArray arr;
            switch (t){
                case DOUBLE:
                    arr = Nd4j.createFromArray(0.0, 1.0, 2.0, 0.5);
                    break;
                case FLOAT:
                    arr = Nd4j.createFromArray(0.0f, 1.0f, 2.0f, 0.5f);
                    break;
                case INT64:
                    arr = Nd4j.createFromArray(0L, 1L, 2L, -1L);
                    break;
                case INT32:
                    arr = Nd4j.createFromArray(0, 1, 2, -1);
                    break;
                case UINT8:
                    arr = Nd4j.createFromArray(0, 1, 2, 3).castTo(DataType.UINT8);
                    break;
                case BOOL:
                    arr = Nd4j.createFromArray(true, false, true, true);
                    break;
                default:
                    throw new RuntimeException();
            }

            NDArray a1 = NDArray.create(arr);
            SerializedNDArray s1 = a1.getAs(SerializedNDArray.class);
            NDArray a2 = NDArray.create(s1);
            Tensor<?> tensor = a2.getAs(Tensor.class);
            NDArray a3 = NDArray.create(tensor);
            assertEquals(a1, a3);

            INDArray arr2 = a3.getAs(INDArray.class);

            assertEquals(arr, arr2);
        }

    }

}
