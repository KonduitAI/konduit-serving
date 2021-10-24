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

import ai.konduit.serving.pipeline.api.data.*;
import org.junit.Test;
import org.nd4j.common.resources.Resources;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataJsonTest {

    @Test
    public void testBasic(){

        for(ValueType vt : ValueType.values()){

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
                    d = Data.singleton("myKey", Image.create(Resources.asFile("data/5_32x32.png")));
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
                    d = Data.singletonList("myKey", Arrays.asList("some", "list", "values"), ValueType.STRING);
                    break;
                case BOUNDING_BOX:
                    d = Data.singleton("myKey", BoundingBox.create(0.5, 0.4, 0.9, 1.0));
                    d.put("myKey2", BoundingBox.createXY(0.1, 1, 0.2, 0.9, "label", 0.7));
                    break;
                case POINT:
                    d = Data.singleton("myKey", Point.create(0.1, 0.2, "foo", 1.0));
                    d.put("myKey2", Point.create(0.1, 0.2, 0.3, "bar", 0.5));
                    d.put("myKey3", Point.create(new double[]{0.1, 0.2, 0.3, 0.4, 0.5}, "spam", 0.2));
                    d.put("myKey4", Point.create(0.9, 0.8, 0.7, 0.6, 0.4, 0.5));
                    break;
                case BYTEBUFFER:
                    d = Data.singleton("myKey", ByteBuffer.wrap(new byte[]{1}));
                    break;
                case NONE:
                    d = Data.singleton("myKey","null");
                    break;
                default:
                    throw new RuntimeException();
            }

            String s = d.toJson();
            System.out.println(s);

            Data d2 = Data.fromJson(s);
            assertEquals(d.get("myKey"), d2.get("myKey"));
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

    @Test
    public void testList(){

        for(ValueType vt : ValueType.values()) {
            if (vt == ValueType.LIST) {
                //TODO NESTED LIST SERIALIZATION NOT YET IMPLEMENTED
                System.out.println("SKIPPING: " + vt);
                continue;
            }

            Data d;
            switch (vt){
                case STRING:
                    List<String> strList = Arrays.asList("test", "string", "list");
                    d = Data.singletonList("key", strList, ValueType.STRING);
                    break;
                case BYTES:
                    List<byte[]> bList = Arrays.asList(new byte[]{0,1,2}, new byte[]{9,8,7});
                    d = Data.singletonList("key", bList, ValueType.BYTES);
                    break;
                case DOUBLE:
                    List<Double> dList = Arrays.asList(0.0, 1.0, 2.0);
                    d = Data.singletonList("key", dList, ValueType.DOUBLE);
                    break;
                case INT64:
                    List<Long> lList = Arrays.asList(0L, 1L, 2L);
                    d = Data.singletonList("key", lList, ValueType.INT64);
                    break;
                case BOOLEAN:
                    List<Boolean> boolList = Arrays.asList(false, true, false);
                    d = Data.singletonList("key", boolList, ValueType.BOOLEAN);
                    break;
                case IMAGE:
                    File f = Resources.asFile("data/5_32x32.png");
                    List<Image> imgList = Arrays.asList(Image.create(f), Image.create(f));
                    d = Data.singletonList("key", imgList, ValueType.IMAGE);
                    break;
                case NDARRAY:
                    List<NDArray> ndList = Arrays.asList(NDArray.create(new float[]{1,2,3}), NDArray.create(new float[]{4,5,6}));
                    d = Data.singletonList("key", ndList, ValueType.NDARRAY);
                    break;
                case DATA:
                    List<Data> dataList = Arrays.asList(Data.singleton("key", "value"), Data.singletonList("key", Arrays.asList("string", "list"), ValueType.STRING));
                    d = Data.singletonList("key", dataList, ValueType.DATA);
                    break;
                case BOUNDING_BOX:
                    List<BoundingBox> bbList = Arrays.asList(BoundingBox.createXY(0.2, 0.4, 0.7, 0.9, "myLabel", 0.8),
                            BoundingBox.create(0.4, 0.5, 0.1, 0.3, "otherlabel", 0.99));
                    d = Data.singletonList("key", bbList, ValueType.BOUNDING_BOX);
                    break;
                case POINT:
                    List<Point> pList = Arrays.asList(
                            Point.create(0.1, 0.2, "foo", 1.0),
                            Point.create(0.1, 0.2, 0.3, "bar", 0.5),
                            Point.create(new double[]{0.1, 0.2, 0.3, 0.4, 0.5}, "spam", 0.2),
                            Point.create(0.9, 0.8, 0.7, 0.6, 0.4, 0.5)
                    );
                    d = Data.singletonList("key", pList, ValueType.POINT);
                    break;
                case BYTEBUFFER:
                    List<ByteBuffer> byteBuffers = Arrays.asList(
                            ByteBuffer.wrap(new byte[]{1})
                    );
                    d = Data.singletonList("key",byteBuffers,ValueType.BYTEBUFFER);
                    break;
                case NONE:
                    d = Data.singleton("key","null");
                    break;

                default:
                    throw new RuntimeException();
            }

            String json = d.toJson();

            System.out.println("======================================");
            System.out.println(json);

            Data dJson = Data.fromJson(json);
            assertEquals(d, dJson);
        }
    }


    @Test
    public void testJsonMetaData(){
        Data d = Data.singleton("myKey", "myValue");
        Data meta = JData.builder()
                .add("someMeta", "someValue")
                .add("otherMeta", 10.0)
                .build();

        d.setMetaData(meta);


        String json = d.toJson();
//        System.out.println(json);
        Data d2 = Data.fromJson(json);
        assertEquals(d, d2);

        Data meta2 = d2.getMetaData();
        assertEquals(meta ,meta2);
    }
}
