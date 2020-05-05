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

import ai.konduit.serving.pipeline.impl.data.wrappers.IntValue;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.extern.slf4j.Slf4j;
import ai.konduit.serving.pipeline.api.Data;
import org.apache.commons.compress.utils.Lists;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Slf4j
public class DataTest {

    private final String KEY = "stringData";
    private final String VALUE = "Some string data";

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

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

    /*
    @Test
    public void testImageData() {
        INDArray image = Nd4j.create(1,10,10,20);
        Image input = new Image(image, 1,1,1);
        Data container = JData.makeData(KEY, input);
        assertEquals(input, container.getImage(KEY));
    }*/

    /*
    @Test
    public void testINDArrayData() {
        INDArray input = Nd4j.rand(10,100, 1024);
        Data container = JData.makeData(KEY, input);
        assertEquals(input, container.getNDArray(KEY));
    }*/

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
    public void testSerde() {
        Data someData = JData.singleton(KEY, Long.valueOf(200));
        try {
            someData.save(new File("temp"));
        } catch (IOException e) {
            log.error("IOException in DataTest.testSerde()", e);
        }
        Data restoredData = null;
        try {
            restoredData = JData.fromFile(new File("temp"));
        } catch (IOException e) {
            log.error("IOException in DataTest.testSerde()", e);
        }
        assertEquals(someData.get(KEY), restoredData.get(KEY));
    }

    @Test
    public void testConvertToBytes() {
        JData someData = (JData) JData.singleton(KEY, Long.valueOf(200));
        byte[] output = someData.asBytes();
        assert(output != null);
    }

    @Test
    public void testConvertToJson() {
        Data someData = (JData) JData.singleton(KEY, Long.valueOf(200));
        String jsonStr = someData.toJson();

        String expected = "{\n" +
                "  \"iValue\": \"200\",\n" +
                "  \"type\": \"INT64\"\n" +
                "}";
        assertEquals(expected, jsonStr);
    }

    @Test
    public void testLists() {
        Data someData = (JData) JData.singleton(KEY, Collections.singletonList(Long.valueOf(200)));
        List<?> someList = someData.getList(KEY, ValueType.INT64);
        assertEquals(1, someList.size());
        assertEquals(200L, someList.get(0));

        List<String> strings = new ArrayList<>();
        strings.add("one");
        strings.add("two");
        strings.add("three");
        Data listOfStrings = JData.singleton(KEY, strings);
        List<?> actual = listOfStrings.getList(KEY, ValueType.STRING);
        assertEquals(strings, actual);
    }

    @Test
    public void testsNumericLists() {
        final long LIST_SIZE = 6;

        List<Object> numbers = Lists.newArrayList();
        for (long i = 0; i < LIST_SIZE; ++i) {
            numbers.add(i);
        }
        Data listOfNumbers = new JData.DataBuilder().
                add(KEY, numbers).
                build();

        List<?> actual = listOfNumbers.getList(KEY);
        assertEquals(numbers, actual);
        for (int i = 0 ; i < LIST_SIZE; ++i) {
            assertEquals(numbers.get(i), actual.get(i));
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
}
