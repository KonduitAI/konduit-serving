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

package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JavaFormatsTest {

    public static Set<Class> allToTest = new LinkedHashSet<>();
    static {
        allToTest.addAll(Arrays.asList(
                float[].class,
                float[][].class,
                float[][][].class,
                float[][][][].class,
                float[][][][][].class,
                double[].class,
                double[][].class,
                double[][][].class,
                double[][][][].class,
                double[][][][][].class,
                /*byte[].class,
                byte[][].class,
                byte[][][].class,
                byte[][][][].class,
                byte[][][][][].class*/
                short[].class,
                short[][].class,
                short[][][].class,
                short[][][][].class,
                short[][][][][].class,
                int[].class,
                int[][].class,
                int[][][].class,
                int[][][][].class,
                int[][][][][].class,
                long[].class,
                long[][].class,
                long[][][].class,
                long[][][][].class,
                long[][][][][].class
        ));

    }

    @Test
    public void testConversion(){
        //Test conversion: from X to SerializedNDArray, and back
        //Also Data with an NDArray with X - to JSON and back; to protobuf and back

        for(Class c : allToTest){
            Object o = getData(c);

            NDArray arr = NDArray.create(o);
            Data d = Data.singleton("key", arr);

            SerializedNDArray x = arr.getAs(SerializedNDArray.class);
            NDArray arr2 = NDArray.create(x);
            assertEquals(arr, arr2);
            Object o2 = arr2.getAs(c);
            if(o instanceof Object[]){
                Arrays.deepEquals((Object[])o, (Object[])o2);
            } else {
                //TODO - primitive array - float[], etc
            }


            String json = d.toJson();
            Data d2 = Data.fromJson(json);
            assertEquals(d, d2);


            //TODO test YAML also


            //TODO Protobuf, file, inputstream etc tests

        }



    }

    public Object getData(Class c){
        if(c == float[].class){
            return new float[]{1,2,3};
        } else if(c == float[][].class){
            return new float[][]{{1,2},{3,4}};
        } else if(c == float[][][].class){
            return new float[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == float[][][][].class){
            return new float[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == float[][][][][].class){
            float[][][][] f4 = new float[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new float[][][][][]{f4, f4};
        } else if(c == double[].class){
            return new double[]{1,2,3};
        } else if(c == double[][].class){
            return new double[][]{{1,2},{3,4}};
        } else if(c == double[][][].class){
            return new double[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == double[][][][].class){
            return new double[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == double[][][][][].class){
            double[][][][] f4 = new double[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new double[][][][][]{f4, f4};
        } /*else if(c == byte[].class){
            return new byte[]{1,2,3};
        } else if(c == byte[][].class){
            return new byte[][]{{1,2},{3,4}};
        } else if(c == byte[][][].class){
            return new byte[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == byte[][][][].class){
            return new byte[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == byte[][][][][].class){
            byte[][][][] f4 = new byte[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new byte[][][][][]{f4, f4};
        } */ else if(c == short[].class){
            return new short[]{1,2,3};
        } else if(c == short[][].class){
            return new short[][]{{1,2},{3,4}};
        } else if(c == short[][][].class){
            return new short[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == short[][][][].class){
            return new short[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == short[][][][][].class){
            short[][][][] f4 = new short[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new short[][][][][]{f4, f4};
        } else if(c == int[].class){
            return new int[]{1,2,3};
        } else if(c == int[][].class){
            return new int[][]{{1,2},{3,4}};
        } else if(c == int[][][].class){
            return new int[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == int[][][][].class){
            return new int[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == int[][][][][].class){
            int[][][][] f4 = new int[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new int[][][][][]{f4, f4};
        } else if(c == long[].class){
            return new long[]{1,2,3};
        } else if(c == long[][].class){
            return new long[][]{{1,2},{3,4}};
        } else if(c == long[][][].class){
            return new long[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};
        } else if(c == long[][][][].class){
            return new long[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
        } else if( c == long[][][][][].class){
            long[][][][] f4 = new long[][][][]{{{{1,2},{3,4}},{{5,6},{7,8}}}, {{{9,10},{11,12}},{{13,14},{15,16}}}};
            return new long[][][][][]{f4, f4};
        }

        throw new RuntimeException("Data type not supported for " + c.toString());
    }

}
