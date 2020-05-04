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
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import static org.junit.Assert.*;

public class ConverterTest {

    @Test
    public void testNDArrayToFloat(){
        float[] f1 = new float[]{1,2,3};
        INDArray iArr1 = Nd4j.createFromArray(f1);
        NDArray arr1 = NDArray.create(iArr1);
        float[] converted1 = arr1.getAs(float[].class);
        assertArrayEquals(f1, converted1, 0.0f);

        float[][] f2 = new float[][]{{1,2},{3,4}};
        INDArray iArr2 = Nd4j.createFromArray(f2);
        NDArray arr2 = NDArray.create(iArr2);
        float[][] converted2 = arr2.getAs(float[][].class);
        assertEquals(f2.length, converted2.length);
        for( int i=0; i<f2.length; i++ ){
            assertArrayEquals(f2[i], converted2[i], 0.0f);
        }


        float[][][] f3 = new float[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        INDArray iArr3 = Nd4j.createFromArray(f3);
        NDArray arr3 = NDArray.create(iArr3);
        float[][][] converted3 = arr3.getAs(float[][][].class);
        assertEquals(f3.length, converted3.length);
        for( int i=0; i<f3.length; i++ ){
            assertEquals(f3[i].length, converted3[i].length);
            for( int j=0; j<f3[i].length; j++ ){
                assertArrayEquals(f3[i][j], converted3[i][j], 0.0f);
            }
        }

        float[][][][] f4 = new float[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        INDArray iArr4 = Nd4j.createFromArray(f4);
        NDArray arr4 = NDArray.create(iArr4);
        float[][][][] converted4 = arr4.getAs(float[][][][].class);
        assertEquals(f4.length, converted4.length);
        for( int i=0; i<f4.length; i++ ){
            assertEquals(f4[i].length, converted3[i].length);
            for( int j=0; j<f4[i].length; j++ ){
                for( int k=0; k<f4[j].length; k++ ) {
                    assertArrayEquals(f4[i][j][k], converted4[i][j][k], 0.0f);
                }
            }
        }


        //Check conversion from
        NDArray fArr1 = NDArray.create(f1);
        assertTrue(fArr1.get() instanceof float[]);
        INDArray ndConverted1 = fArr1.getAs(INDArray.class);
        assertEquals(iArr1, ndConverted1);

        NDArray fArr2 = NDArray.create(f2);
        assertTrue(fArr2.get() instanceof float[][]);
        INDArray ndConverted2 = fArr2.getAs(INDArray.class);
        assertEquals(iArr2, ndConverted2);

        NDArray fArr3 = NDArray.create(f3);
        assertTrue(fArr3.get() instanceof float[][][]);
        INDArray ndConverted3 = fArr3.getAs(INDArray.class);
        assertEquals(iArr3, ndConverted3);

        NDArray fArr4 = NDArray.create(f4);
        assertTrue(fArr4.get() instanceof float[][][][]);
        INDArray ndConverted4 = fArr4.getAs(INDArray.class);
        assertEquals(iArr4, ndConverted4);
    }
}
