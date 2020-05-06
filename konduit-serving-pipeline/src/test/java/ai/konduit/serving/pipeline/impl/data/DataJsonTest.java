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
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataJsonTest {

    @Test
    public void testBasic(){

        for(ValueType vt : ValueType.values()){
            if(vt == ValueType.IMAGE || vt == ValueType.LIST || vt == ValueType.NDARRAY){
                System.out.println("SKIPPING: " + vt);
                continue;
            }

            System.out.println(" ----- " + vt + " -----");

            Data d;
            switch (vt){
                case NDARRAY:
                    d = Data.singleton("myKey", NDArray.create(new float[]{0,1,2}));
                    break;
                case STRING:
                    d = Data.singleton("myKey", "myString");
                    break;
                case BYTES:
                    d = Data.singleton("myKey", new byte[]{0,1,2});
                    break;
                case IMAGE:
                    d = null;
                    break;
                case DOUBLE:
                    d = Data.singleton("myKey", 1.0);
                    break;
                case INT64:
                    d = Data.singleton("myKey", 1L);
                    break;
                case BOOLEAN:
                    d = Data.singleton("myKey", true);
                    break;
                case DATA:
                    d = Data.singleton("myKey", Data.singleton("myInnerKey", "myInnerValue"));
                    break;
                case LIST:
                    d = null;
                    break;
                default:
                    d = null;
            }

            String s = d.toJson();

            Data d2 = Data.fromJson(s);
            assertEquals(d, d2);

            System.out.println(s);
        }
    }

    @Test
    public void testNestedData(){

        Data dInner = Data.singleton("inner", 1.0);
        Data dOuter = Data.singleton("outer", dInner);

        String json = dOuter.toJson();
        System.out.println(json);

        Data dOuterJson = Data.fromJson(json);

        assertEquals(dOuter, dOuterJson);

        Data dInnerJson = dOuterJson.getData("outer");
        assertEquals(dInner, dInnerJson);
    }

    @Ignore     //NO WAY TO PUT LISTS INTO DATA YET
    @Test
    public void testList(){

        for(ValueType vt : ValueType.values()) {
            if (vt == ValueType.IMAGE || vt == ValueType.DATA || vt == ValueType.LIST || vt == ValueType.NDARRAY) {
                System.out.println("SKIPPING: " + vt);
                continue;
            }

            Data d;
            switch (vt){
                case STRING:
                    List<String> strList = Arrays.asList("test", "string", "list");

                    break;
                case BYTES:
                    break;
                case DOUBLE:
                    break;
                case INT64:
                    break;
                case BOOLEAN:
                    break;
            }


        }

    }

}
