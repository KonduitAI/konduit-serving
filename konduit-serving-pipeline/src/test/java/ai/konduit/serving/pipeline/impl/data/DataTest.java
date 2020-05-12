/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import org.apache.commons.compress.utils.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.resources.Resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ai.konduit.serving.pipeline.impl.data.JData.empty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class DataTest {

    private final String KEY = "stringData";
    private final String VALUE = "Some string data";

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testStringData() {

        Data container = JData.singleton(KEY, VALUE);
        String value = container.getString(KEY);
        assertEquals(VALUE, value);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoData() {
        BigDecimal notSupportedValue = BigDecimal.ONE;
        Data container = JData.singleton(KEY, notSupportedValue);
    }

    @Test(expected = ValueNotFoundException.class)
    public void testAbsentData() {
        Data container = JData.singleton(KEY, VALUE);
        String value = container.getString("no_data_for_this_key");
        assertEquals(VALUE, value);
    }

    @Test
    public void testBooleanData() {
        boolean value = true;
        Data container = JData.singleton(KEY, value);
        assertEquals(true, container.getBoolean(KEY));
    }

    @Test
    public void testBytesData() {
        byte[] input = new byte[]{1,2,3,4,5};
        Data container = JData.singleton(KEY, input);
        assertArrayEquals(input, container.getBytes(KEY));
    }

    @Test
    public void testDoubleData() {
        Double input = 1.0;
        Data container = JData.singleton(KEY, input);
        assertEquals(input, container.getDouble(KEY), 1e-4);
    }

    @Test
    public void testIntData() {
        long data = 100;
        Data container = JData.singleton(KEY, data);
        assertEquals(data, container.getLong(KEY));
    }

    @Test
    public void testEmbeddedData() {
        Data stringContainer = JData.singleton("stringData", "test");
        Data booleanContainer = JData.singleton("boolData", false);
        booleanContainer.put("level1", stringContainer);
        Data ndContainer = JData.singleton("bytesData", new byte[10]);
        ndContainer.put("level2", booleanContainer);

        Data layeredContainer = JData.singleton("upperLevel", ndContainer);
    }

    @Test
    public void testSerde() throws IOException {
        Data someData = JData.singleton(KEY, Long.valueOf(200));
        ProtoData protoData = someData.toProtoData();
        File testFile = testDir.newFile();
        protoData.save(testFile);
        Data restoredData = Data.fromFile(testFile);
        assertEquals(protoData.get(KEY), restoredData.get(KEY));
    }

    @Test
    public void testConvertToBytes() {
        Data longData = Data.singleton(KEY, Long.valueOf(200));
        byte[] output = longData.asBytes();
        assert(output != null);

        /*Data intData = Data.singleton(KEY, Integer.valueOf(20));
        output = intData.asBytes();
        assert(output != null);*/
    }

    @Test
    public void testInt32Conversion() {
        Data intData = Data.singleton(KEY, Integer.valueOf(200));
        Data longData = Data.singleton(KEY, Long.valueOf(200L));
        assertEquals(intData.get(KEY), longData.get(KEY));
    }

    @Test
    public void testFloatConversion() {
        Data floatData = Data.singleton(KEY, Float.valueOf(200));
        Data doubleData = Data.singleton(KEY, Double.valueOf(200.0));
        assertEquals(floatData.get(KEY), doubleData.get(KEY));
    }


    @Test
    public void testLists() {
        Data someData = (JData) JData.singletonList(KEY, Collections.singletonList(Long.valueOf(200)), ValueType.INT64);
        List<?> someList = someData.getList(KEY, ValueType.INT64);
        assertEquals(1, someList.size());
        assertEquals(200L, someList.get(0));
        assertEquals(ValueType.INT64, someData.listType(KEY));

        List<String> strings = new ArrayList<>();
        strings.add("one");
        strings.add("two");
        strings.add("three");
        Data listOfStrings = JData.singletonList(KEY, strings, ValueType.STRING);
        assertEquals(ValueType.LIST, listOfStrings.type(KEY));
        assertEquals(ValueType.STRING, listOfStrings.listType(KEY));

        List<?> actual = listOfStrings.getList(KEY, ValueType.STRING);
        assertEquals(strings, actual);
    }

    @Test
    public void testListOfLists() {
        List<Long> innerData = new ArrayList<>();
        for (long i = 0; i < 6; ++i) {
            innerData.add(i);
        }
        List<List<Long>> data = new ArrayList<>();
        data.add(innerData);

        Data listOfLists = Data.singletonList(KEY, data, ValueType.INT64);
        List<?> actual = listOfLists.getList(KEY, ValueType.LIST);
        assertEquals(1, actual.size());
        assertEquals(6, ((List<Long>)actual.get(0)).size());
        for (long i = 0; i < 6; ++i) {
            assertEquals(i, (long)((List<Long>)actual.get(0)).get((int)i));
        }
        assertEquals(ValueType.INT64, listOfLists.listType(KEY));

        List<Long> innerBigData = new ArrayList<>();
        for (long i = 0; i < 100; ++i) {
            innerBigData.add(i);
        }
        data.add(innerBigData);
        Data listOfTwoLists = Data.singletonList(KEY, data, ValueType.INT64);
        assertEquals(ValueType.LIST, listOfTwoLists.type(KEY));
        assertEquals(ValueType.INT64, listOfTwoLists.listType(KEY));

        List<?> bigData = listOfTwoLists.getList(KEY, ValueType.INT64);
        assertEquals(2,bigData.size());
        assertEquals(6, ((List<?>)bigData.get(0)).size());
        assertEquals(100, ((List<?>)bigData.get(1)).size() );
    }

    @Test
    public void testWrongValueTypeForList() {
        List<String> strings = new ArrayList<>();
        strings.add("one");
        strings.add("two");
        strings.add("three");
        // TODO: Value type check for lists is missing.
        Data brokenList = Data.singletonList(KEY, strings, ValueType.BOOLEAN);
        assertEquals(ValueType.BOOLEAN, brokenList.listType(KEY));
    }

    @Test
    public void testsNumericLists() {
        final long LIST_SIZE = 6;

        List<Long> numbers = Lists.newArrayList();
        for (long i = 0; i < LIST_SIZE; ++i) {
            numbers.add(i);
        }
        Data listOfNumbers = new JData.DataBuilder().
                addListInt64(KEY, numbers).
                build();
        assertEquals(ValueType.INT64, listOfNumbers.listType(KEY));

        List<?> actual = listOfNumbers.getList(KEY, ValueType.INT64);
        assertEquals(numbers, actual);
        for (int i = 0 ; i < LIST_SIZE; ++i) {
            assertEquals(numbers.get(i), actual.get(i));
        }
    }

    @Test
    public void testBooleanList() {
        final long LIST_SIZE = 6;

        List<Boolean> data = Lists.newArrayList();
        for (int i = 0; i < LIST_SIZE; ++i) {
            data.add(i % 2 == 0);
        }
        Data listOfBoolean = Data.singletonList(KEY, data, ValueType.BOOLEAN);
        assertEquals(ValueType.BOOLEAN, listOfBoolean.listType(KEY));

        List<?> actual = listOfBoolean.getList(KEY, ValueType.BOOLEAN);
        for (int i = 0 ; i < LIST_SIZE; ++i) {
            assertEquals(data.get(i), actual.get(i));
        }
    }

    @Test
    public void testDoubleLists() {
        final long LIST_SIZE = 6;

        List<Double> data = Lists.newArrayList();
        for (int i = 0; i < LIST_SIZE; ++i) {
            data.add(Double.valueOf(i));
        }

        Data listOfDouble = new JData.DataBuilder().addListDouble(KEY, data).build();
        assertEquals(ValueType.DOUBLE, listOfDouble.listType(KEY));

        List<?> actual = listOfDouble.getList(KEY, ValueType.DOUBLE);
        for (int i = 0 ; i < LIST_SIZE; ++i) {
            assertEquals(data.get(i), actual.get(i));
        }
    }

    @Test
    public void testBuilderVSFactory() {
        JData built = new JData.DataBuilder().
                add("key1", true).
                add("key2","null").
                build();

        // Normally don't need this cast. Just for asserts below and is subject to change.
        JData made = (JData)JData.singleton("key1", true);
        made.put("key2", "null");

        JData madeFromEmpty = (JData) JData.empty();
        madeFromEmpty.put("key1", true);
        madeFromEmpty.put("key2", "null");

        assertEquals(built.getDataMap().get("key1").get(), made.getDataMap().get("key1").get());
        assertEquals(built.getDataMap().get("key2").get(), made.getDataMap().get("key2").get());

        assertEquals(built.getDataMap().get("key1").get(), madeFromEmpty.getDataMap().get("key1").get());
        assertEquals(built.getDataMap().get("key2").get(), madeFromEmpty.getDataMap().get("key2").get());
    }

    @Test
    public void testJsonConversion() {
        Data someData = Data.singleton(KEY, "test");
        String jsonString = someData.toJson();

        Data someDataFromJson = Data.fromJson(jsonString);

        String actualJsonStr = someDataFromJson.toJson();
        assertEquals(jsonString, actualJsonStr);
        assertEquals(someData.get(KEY), someDataFromJson.get(KEY));
    }

    @Test
    public void testBytesSerde() {
        JData someData = (JData)Data.singleton(KEY, "testString");
        byte[] someBytes = someData.asBytes();
        JData restoredData = (JData) Data.fromBytes(someBytes);
        assertEquals(someData.get(KEY), restoredData.get(KEY));
    }

    @Test
    public void testStreamsSerde() throws IOException {
        JData someData = (JData)Data.singleton(KEY, "testString");
        Data restored = empty();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            someData.write(baos);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                restored = Data.fromStream(bais);
            }
        }
        assertEquals(someData.get(KEY), restored.get(KEY));
    }

    @Test
    public void testImageData() {
        File f = Resources.asFile("data/5_32x32.png");
        Image i = Image.create(f);

        Data imageData = Data.singleton(KEY, i);

        Png p = i.getAs(Png.class);
        byte[] origBytes = p.getBytes();

        Png p2 = ((Image)imageData.get(KEY)).getAs(Png.class);
        byte[] actualBytes = p2.getBytes();

        assertEquals(origBytes, actualBytes);
    }

    @Test
    public void testImageSerde() throws IOException {
        File f = Resources.asFile("data/5_32x32.png");
        Image i = Image.create(f);

        Data imageData = Data.singleton(KEY, i);
        File serFile = testDir.newFile();
        imageData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(imageData.get(KEY), restoredData.get(KEY));
    }

    @Test
    public void testNDArraySerde() throws IOException {
        float[] rawData = {1, 3, 6, 7, 8, 10, 4, 3, 2, 4};
        long[] shape = {1, 10};
        NDArray ndArray = NDArray.create(rawData);
        //SerializedNDArray serializedNDArray = ndArray.getAs(SerializedNDArray.class);
        Data ndData = Data.singleton(KEY, ndArray);
        File serFile = testDir.newFile();
        ndData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(ndData.get(KEY), restoredData.get(KEY));
    }

    @Test
    public void testImageListSerde() throws IOException {
        List<Image> imageList = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            File f = Resources.asFile("data/5_32x32.png");
            imageList.add(Image.create(f));
        }

        Data imageData = Data.singletonList(KEY, imageList, ValueType.IMAGE);
        File serFile = testDir.newFile();
        imageData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(imageData.getList(KEY, ValueType.IMAGE),
                     restoredData.getList(KEY, ValueType.IMAGE));
    }

    @Test
    public void testNDArraysListSerde() throws IOException {
        List<NDArray> arrays = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            float[] rawData = {1, 3, 6, 7, 8, 10, 4, 3, 2, 4};
            arrays.add(NDArray.create(rawData));
        }
        Data ndData = Data.singletonList(KEY, arrays, ValueType.NDARRAY);
        File serFile = testDir.newFile();
        ndData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(ndData.getList(KEY, ValueType.NDARRAY),
                restoredData.getList(KEY, ValueType.NDARRAY));
    }

    @Test
    public void testStringsListSerde() throws IOException {
        List<String> strData = new ArrayList<>();
        strData.add("one");
        strData.add("two");
        strData.add("three");
        Data stringsData = Data.singletonList(KEY, strData, ValueType.STRING);
        File serFile = testDir.newFile();
        stringsData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(stringsData.getList(KEY, ValueType.STRING),
                     restoredData.getList(KEY, ValueType.STRING));
    }

    @Test
    public void testDoubleListsSerde() throws IOException {
        List<Double> rawData = new ArrayList<>();
        rawData.add(1.);
        rawData.add(20.);
        rawData.add(50.);
        rawData.add(0.78767);

        Data doubleData = Data.singletonList(KEY, rawData, ValueType.DOUBLE);
        File serFile = testDir.newFile();
        doubleData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(doubleData.getList(KEY, ValueType.DOUBLE),
                        restoredData.getList(KEY, ValueType.DOUBLE));

    }

    @Test
    public void testBooleanListsSerde() throws IOException {
        List<Boolean> rawData = new ArrayList<>();
        rawData.add(true);
        rawData.add(false);
        rawData.add(true);
        rawData.add(true);

        Data boolData = Data.singletonList(KEY, rawData, ValueType.BOOLEAN);
        File serFile = testDir.newFile();
        boolData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(boolData.getList(KEY, ValueType.DOUBLE),
                restoredData.getList(KEY, ValueType.DOUBLE));

    }

    @Test
    public void testInt64ListsSerde() throws IOException {
        List<Long> rawData = new ArrayList<>();
        for (long l = 0; l < 10000; ++l) {
            rawData.add(l);
        }

        Data intData = Data.singletonList(KEY, rawData, ValueType.INT64);
        File serFile = testDir.newFile();
        intData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        assertEquals(intData.getList(KEY, ValueType.DOUBLE),
                restoredData.getList(KEY, ValueType.DOUBLE));

    }

    @Test
    public void testDataSerde() throws IOException {
        List<String> strings = new ArrayList<>();
        strings.add("one");
        strings.add("two");
        strings.add("three");
        strings.add("four");

        final String dataAsValue = "dataAsValue";
        Data strData = Data.singletonList(KEY, strings, ValueType.STRING);
        Data wrapppedData = Data.singleton(dataAsValue, strData);

        File serFile = testDir.newFile();
        wrapppedData.save(serFile);
        Data restoredData = Data.fromFile(serFile);
        Data expectedData = (Data)wrapppedData.get(dataAsValue);
        Data actualData = (Data)restoredData.get(dataAsValue);
        assertEquals(expectedData, actualData);
        assertEquals(expectedData.get(KEY), actualData.get(KEY));
    }

    @Test
    public void testMetaDataSerde() {

    }

}
