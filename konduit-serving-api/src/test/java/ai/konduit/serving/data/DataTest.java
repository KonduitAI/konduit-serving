/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2020 Konduit AI.
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
package ai.konduit.serving.data;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

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

        Data container = JData.makeData(KEY, VALUE);
        String value = container.getString(KEY);
        assertEquals(VALUE, value);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoData() {
        BigDecimal notSupportedValue = BigDecimal.ONE;
        Data container = JData.makeData(KEY, notSupportedValue);
    }

    @Test(expected = ValueNotFoundException.class)
    public void testAbsentData() {
        Data container = JData.makeData(KEY, VALUE);
        String value = container.getString("no_data_for_this_key");
        assertEquals(VALUE, value);
    }

    @Test
    public void testBooleanData() {
        boolean value = true;
        Data container = JData.makeData(KEY, value);
        assertEquals(true, container.getBoolean(KEY));
    }

    @Test
    public void testBytesData() {
        byte[] input = Nd4j.rand(2, 512).data().asBytes();
        Data container = JData.makeData(KEY, input);
        assertArrayEquals(input, container.getBytes(KEY));
    }

    @Test
    public void testDoubleData() {
        Double input = 1.0;
        Data container = JData.makeData(KEY, input);
        assertEquals(input, container.getDouble(KEY), 1e-4);
    }

    @Test
    public void testImageData() {
        INDArray image = Nd4j.create(1,10,10,20);
        Image input = new Image(image, 1,1,1);
        Data container = JData.makeData(KEY, input);
        assertEquals(input, container.getImage(KEY));
    }

    @Test
    public void testINDArrayData() {
        INDArray input = Nd4j.rand(10,100, 1024);
        Data container = JData.makeData(KEY, input);
        assertEquals(input, container.getNDArray(KEY));
    }

    @Test
    public void testIntData() {
        long data = 100;
        Data container = JData.makeData(KEY, data);
        assertEquals(data, container.getLong(KEY));
    }

    @Test
    public void testEmbeddedData() {
        Data stringContainer = JData.makeData("stringData", "test");
        Data booleanContainer = JData.makeData("boolData", false);
        booleanContainer.put("level1", stringContainer);
        Data ndContainer = JData.makeData("ndData", Nd4j.rand(1,19));
        ndContainer.put("level2", booleanContainer);

        Data layeredContainer = JData.makeData("upperLevel", ndContainer);
    }

    @Ignore
    @Test
    public void testSerde() {
        Data someData = JData.makeData(KEY, Long.valueOf(200));
        someData.save(new File("temp"));
        try {
            Data restoredData = JData.fromFile(new File("temp"));
        } catch (IOException e) {
            log.error("IOException in DataTest.testSerde()", e);
        }
    }

    @Test
    public void testBuilderVSFactory() {
        JData built = new JData.DataBuilder().
                add("key1", true).
                add("key2","null").
                builld();

        // Normally don't need this cast. Just for asserts below and is subject to change.
        JData made = (JData)JData.makeData("key1", true);
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
