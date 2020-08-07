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
package ai.konduit.serving.python.util;

import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.image.PngImage;
import com.google.flatbuffers.ByteBufferUtil;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.datavec.python.Python;
import org.datavec.python.PythonType;
import org.datavec.python.PythonVariables;
import org.junit.Test;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PythonUtilsTest {

    @Test
    public void testListInsert() throws Exception {
        Data data = Data.empty();
        for(ValueType valueType : ValueType.values()) {
            switch(valueType) {
                case STRING:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList("value"),
                            valueType);
                    assertEquals(Arrays.asList("value"),data.getList(valueType.toString(),valueType));
                    break;
                case BYTES:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList("value".getBytes()),
                            valueType);
                    assertArrayEquals("value".getBytes(), (byte[]) data.getList(valueType.toString(),valueType).get(0));
                    break;
                case IMAGE:
                    ClassPathResource classPathResource = new ClassPathResource("data/5_32x32.png");
                    File image = classPathResource.getFile();
                    Image image2 = Image.create(image);
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(image2),
                            valueType);
                    assertEquals(Arrays.asList(image2),data.getList(valueType.toString(),valueType));

                    break;
                case DOUBLE:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(1.0),
                            valueType);
                    assertEquals(Arrays.asList(1.0),data.getList(valueType.toString(),valueType));
                    break;
                case INT64:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(1),
                            valueType);
                    assertEquals(1L,data.getList(valueType.toString(),valueType).get(0));

                    break;
                case BOOLEAN:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(true),
                            valueType);
                    assertEquals(Arrays.asList(true),data.getList(valueType.toString(),valueType));
                    break;
                case BOUNDING_BOX:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(BoundingBox.create(1.0,1.0,1.0,1.0)),
                            valueType);
                    assertEquals(Arrays.asList(BoundingBox.create(1.0,1.0,1.0,1.0)),data.getList(valueType.toString(),valueType));

                    break;
                case POINT:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(Point.create(1,1,1)),
                            valueType);
                    assertEquals(Arrays.asList(Point.create(1,1,1)),data.getList(valueType.toString(),valueType));

                    break;

                case NDARRAY:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(Nd4j.scalar(1.0)),
                            valueType);
                    assertEquals(Arrays.asList(new ND4JNDArray(Nd4j.scalar(1.0))),data.getList(valueType.toString(),valueType));

                    break;

                case BYTEBUFFER:
                    PythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(ByteBuffer.wrap(new byte[]{1})),
                            valueType);
                    assertEquals(Arrays.asList(ByteBuffer.wrap(new byte[]{1})),data.getList(valueType.toString(),valueType));
                    break;
            }
        }
    }


    @Test
    public void testInsertBytes() throws Exception {
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonOutput("len_input",PythonType.TypeName.INT.name())
                .pythonOutput("input", PythonType.TypeName.BYTES.name())
                .outputTypeByteConversion("input",ValueType.BYTES)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();
        for(Map.Entry<String,String> entry : pythonConfig.getPythonOutputs().entrySet()) {
            pythonVariables.add(entry.getKey(),PythonType.valueOf(entry.getValue()));
        }

        pythonVariables.addBytes("input",new BytePointer(new byte[]{1}));
        pythonVariables.addInt("len_input",1);

        PythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        byte[] inputs = input.getBytes("input");
        assertArrayEquals(new byte[]{1},inputs);
    }


    @Test
    public void testInsertImage() throws Exception {
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonOutput("len_input",PythonType.TypeName.INT.name())
                .pythonOutput("input", PythonType.TypeName.BYTES.name())
                .outputTypeByteConversion("input",ValueType.IMAGE)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();
        for(Map.Entry<String,String> entry : pythonConfig.getPythonOutputs().entrySet()) {
            pythonVariables.add(entry.getKey(),PythonType.valueOf(entry.getValue()));
        }

        ClassPathResource testImage = new ClassPathResource("data/5_32x32.png");
        File image = testImage.getFile();
        byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(image));
        Image assertionImage = Image.create(image);
        pythonVariables.addBytes("input",new BytePointer(imageBytes));
        pythonVariables.addInt("len_input",imageBytes.length);

        PythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);

        Image testImageOutput = input.getImage("input");
        assertEquals(assertionImage.width(),testImageOutput.width());
        assertEquals(assertionImage.height(),testImageOutput.height());

    }

    @Test
    public void testInsertString() throws Exception {
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonOutput("len_input",PythonType.TypeName.INT.name())
                .pythonOutput("input", PythonType.TypeName.BYTES.name())
                .outputTypeByteConversion("input",ValueType.STRING)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();
        for(Map.Entry<String,String> entry : pythonConfig.getPythonOutputs().entrySet()) {
            pythonVariables.add(entry.getKey(),PythonType.valueOf(entry.getValue()));
        }

        pythonVariables.addBytes("input",new BytePointer("input".getBytes()));
        pythonVariables.addInt("len_input","input".getBytes().length);

        PythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        String inputs = new String(input.getBytes("input"));
        assertEquals("input",inputs);
    }

    @Test
    public void testInsertByteBuffer() throws Exception {
        PythonConfig pythonConfig = PythonConfig.builder()
                .pythonOutput("len_input",PythonType.TypeName.INT.name())
                .pythonOutput("input", PythonType.TypeName.BYTES.name())
                .outputTypeByteConversion("input",ValueType.BYTEBUFFER)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();
        for(Map.Entry<String,String> entry : pythonConfig.getPythonOutputs().entrySet()) {
            pythonVariables.add(entry.getKey(),PythonType.valueOf(entry.getValue()));
        }

        pythonVariables.addBytes("input",new BytePointer(new byte[]{1}));
        pythonVariables.addInt("len_input",1);

        PythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        ByteBuffer inputs = input.getByteBuffer("input");
        assertEquals(ByteBuffer.wrap(new byte[]{1}),inputs);
    }

    @Test
    public void testIllegalInsertList() {
        PythonUtils.insertListIntoData(
                Data.empty(),"illegal",
                Arrays.asList(Collections.emptyList()),
                ValueType.LIST);
    }

    @Test
    public void testIllegalInsertData() {
        PythonUtils.insertListIntoData(
                Data.empty(),"illegal",
                Arrays.asList(Data.empty()),
                ValueType.DATA);
    }
}
