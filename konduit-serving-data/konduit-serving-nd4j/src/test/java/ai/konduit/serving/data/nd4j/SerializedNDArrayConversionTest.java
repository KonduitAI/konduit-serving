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

package ai.konduit.serving.data.nd4j;

import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.assertEquals;

public class SerializedNDArrayConversionTest {

    @Test
    public void testFloatConversion(){

        for( int rank=1; rank<4; rank++ ){

            NDArray arr;
            INDArray exp;
            switch(rank){
                case 1:
                    arr = NDArray.create(new float[]{1,2,3});
                    exp = Nd4j.createFromArray(arr.getAs(float[].class));
                    break;
                case 2:
                    arr = NDArray.create(new float[][]{{1,2},{3,4}});
                    exp = Nd4j.createFromArray(arr.getAs(float[][].class));
                    break;
                case 3:
                    arr = NDArray.create(new float[][][]{{{1,2},{3,4}},{{5,6},{7,8}}});
                    exp = Nd4j.createFromArray(arr.getAs(float[][][].class));
                    break;
                case 4:
                    arr = NDArray.create(new float[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}});
                    exp = null;
                    break;
                default:
                    throw new RuntimeException();
            }

            SerializedNDArray sa = arr.getAs(SerializedNDArray.class);
            NDArray arr2 = NDArray.create(sa);

            INDArray ia = arr2.getAs(INDArray.class);

            if(exp != null)
                assertEquals(exp, ia);

            NDArray arr3 = NDArray.create(ia);
            SerializedNDArray s2 = arr3.getAs(SerializedNDArray.class);
            assertEquals(sa, s2);
        }
    }
}
