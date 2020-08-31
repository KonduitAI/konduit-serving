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
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.Value;
import ai.konduit.serving.python.DictUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;

import org.bytedeco.javacv.FrameFilter;
import org.junit.Test;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.*;

import ai.konduit.serving.model.PythonConfig;
import org.nd4j.python4j.PythonType;
import org.nd4j.python4j.PythonTypes;
import org.nd4j.python4j.PythonVariable;
import org.nd4j.python4j.PythonVariables;

import static org.junit.Assert.*;

public class KonduitPythonUtilsTest {


    @Test
    public void testCreateValidItemListForPythonVariables() throws Exception {
        for(ValueType valueType : ValueType.values()) {
            List<Object> conversionInput = new ArrayList<>();
            List<Object> assertion = new ArrayList<>();
            switch(valueType) {
                case POINT:
                    Point point = Point.create(1,2);
                    Map<String,Object> pointDict = DictUtils.toPointDict(point);
                    conversionInput.add(point);
                    assertion.add(pointDict);
                    break;
                case BOUNDING_BOX:
                    BoundingBox boundingBox = BoundingBox.create(1,2,3,4);
                    Map<String,Object> boundingBoxDict = DictUtils.toBoundingBoxDict(boundingBox);
                    conversionInput.add(boundingBox);
                    assertion.add(boundingBoxDict);
                    break;
                case BYTES:
                    byte[] input = {1};
                    conversionInput.add(input);
                    assertion.add(input);
                    break;
                case LIST:
                    break;
                case DOUBLE:
                    conversionInput.add(1.0);
                    assertion.add(1.0);
                    break;
                case STRING:
                    conversionInput.add("hello");
                    assertion.add("hello");
                    break;
                case DATA:
                    break;
                case IMAGE:
                    File file = new ClassPathResource("data/5_32x32.png").getFile();
                    Image image = Image.create(file);
                    conversionInput.add(image);
                    byte[] output = FileUtils.readFileToByteArray(file);
                    assertion.add(output);
                    break;
                case INT64:
                    conversionInput.add(1);
                    assertion.add(1);
                    break;
                case BYTEBUFFER:
                    byte[] bytes = {1};
                    conversionInput.add(ByteBuffer.wrap(bytes));
                    assertion.add(bytes);
                    break;
                case NDARRAY:
                    INDArray add = Nd4j.scalar(1.0);
                    NDArray ndArray = NDArray.create(add);
                    conversionInput.add(ndArray);
                    assertion.add(add);
                    break;
                case BOOLEAN:
                    conversionInput.add(true);
                    assertion.add(true);
                    break;
            }

            List<Object> validListForPythonVariables = KonduitPythonUtils.createValidListForPythonVariables(conversionInput, valueType);
            switch(valueType) {
                default:
                    assertEquals("Type failed " + valueType,assertion,validListForPythonVariables);
                    break;
                case BYTES:
                    for(int i = 0; i < assertion.size(); i++) {
                        byte[] assertionBytes = (byte[]) assertion.get(i);
                        byte[] tests = (byte[]) conversionInput.get(i);
                        assertArrayEquals(assertionBytes,tests);
                    }
                    break;
                case BYTEBUFFER:
                    for(int i = 0; i < assertion.size(); i++) {
                        ByteBuffer byteBuffer = (ByteBuffer) conversionInput.get(i);
                        byte[] assertionBytes = (byte[]) assertion.get(i);
                        byte[] tests = byteBuffer.array();
                        assertArrayEquals(assertionBytes,tests);
                    }
                    break;
                case IMAGE:
                    Object firstOutput = validListForPythonVariables.get(0);
                    byte[] bytes = (byte[]) firstOutput;
                    byte[] assertions = (byte[]) validListForPythonVariables.get(0);
                    assertArrayEquals(assertions,bytes);

            }
        }
    }


    @Test
    public void testCreatePythonVariablesFromPythonInputList() throws Exception  {
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        Data data = Data.empty();
        PythonVariables assertion = new PythonVariables();

        for(ValueType valueType : ValueType.values()) {
            //skip data
            if(valueType != ValueType.DATA) {
                PythonIO input = PythonIO.builder()
                        .pythonType(KonduitPythonUtils.typeForValueType(valueType).getName())
                        .secondaryType(valueType)
                        .type(ValueType.LIST)
                        .name(valueType.name().toLowerCase())
                        .build();
                builder.ioInput(input.name(),input);
            }
            switch(valueType) {
                case BOOLEAN:
                    data.putListBoolean(valueType.name().toLowerCase(),Arrays.asList(true));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),true);
                    break;
                case POINT:
                    data.putListPoint(valueType.name().toLowerCase(),Arrays.asList(Point.create(1,2)));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),DictUtils.toPointDict(Point.create(1,2)));
                    break;
                case BOUNDING_BOX:
                    data.putListBoundingBox(valueType.name().toLowerCase(),Arrays.asList(BoundingBox.create(1,2,3,4)));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType), DictUtils.toBoundingBoxDict(BoundingBox.create(1,2,3,4)));
                    break;
                case BYTES:
                    data.putListBytes(valueType.name().toLowerCase(),Arrays.asList(new byte[]{1}));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),new byte[]{1});
                    break;
                case NDARRAY:
                    data.putListNDArray(valueType.name().toLowerCase(),Arrays.asList(NDArray.create(Nd4j.scalar(1.0))));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),Nd4j.scalar(1.0));
                    break;
                case DOUBLE:
                    data.putListDouble(valueType.name().toLowerCase(),Arrays.asList(1.0));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),1.0);
                    break;
                case STRING:
                    data.putListString(valueType.name().toLowerCase(),Arrays.asList("1"));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),"1");
                    break;
                case IMAGE:
                    File imageFile = new ClassPathResource("data/5_32x32.png").getFile();
                    Image image = Image.create(imageFile);
                    data.putListImage(valueType.name().toLowerCase(),Arrays.asList(image));
                    byte[] imageContent = FileUtils.readFileToByteArray(imageFile);
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),imageContent);
                    break;
                case INT64:
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),1);
                    data.putListInt64(valueType.name().toLowerCase(),Arrays.asList(1L));
                    break;
                case BYTEBUFFER:
                    data.putListByteBuffer(valueType.name().toLowerCase(),Arrays.asList(ByteBuffer.wrap(new byte[]{1})));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),new byte[]{1});
                    break;
            }

        }

        PythonConfig config = builder.build();
        PythonVariables createdVariables = KonduitPythonUtils.createPythonVariablesFromDataInput(data,config);
        assertEquals(assertion.size(),createdVariables.size());
        for(int i = 0; i < createdVariables.size(); i++) {
            PythonVariable pythonVariable = createdVariables.get(i);
            PythonVariable assertionVariable = assertion.get(pythonVariable.getName());
            assertNotNull(pythonVariable);
            assertNotNull(assertionVariable);
            ValueType listType = config.getIoInputs().get(pythonVariable.getName()).secondaryType();
            if(KonduitPythonUtils.typeForValueType(listType).equals(PythonTypes.BYTES)) {
                List value = (List) pythonVariable.getValue();
                byte[] createdBytes = (byte[]) value.get(0);
                byte[] assertionBytes = (byte[]) assertionVariable.getValue();
                assertArrayEquals(assertionBytes,createdBytes);
            }
            else
                assertEquals(Arrays.asList(assertionVariable.getValue()),pythonVariable.getValue());
        }
    }

    @Test
    public void testCreatePythonVariablesFromPythonInput() throws  Exception {
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        Data data = Data.empty();
        PythonVariables assertion = new PythonVariables();

        for(ValueType valueType : ValueType.values()) {
            PythonIO input = null;
            //skip data
            if(valueType != ValueType.DATA && valueType != ValueType.NONE) {
                input = PythonIO.builder()
                        .pythonType(KonduitPythonUtils.typeForValueType(valueType).getName())
                        .secondaryType(valueType)
                        .type(valueType)
                        .name(valueType.name().toLowerCase())
                        .build();
                builder.ioInput(input.name(),input);
            }
            switch(valueType) {
                case BOOLEAN:
                    data.put(valueType.name().toLowerCase(),true);
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),true);
                    break;
                case POINT:
                    data.put(valueType.name().toLowerCase(),Point.create(1,2));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),DictUtils.toPointDict(Point.create(1,2)));
                    break;
                case BOUNDING_BOX:
                    data.put(valueType.name().toLowerCase(),BoundingBox.create(1,2,3,4));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType), DictUtils.toBoundingBoxDict(BoundingBox.create(1,2,3,4)));
                    break;
                case BYTES:
                    data.put(valueType.name().toLowerCase(),new byte[]{1});
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),new byte[]{1});
                    break;
                case LIST:
                    input.secondaryType(ValueType.STRING);
                    data.putListString(valueType.name().toLowerCase(),Arrays.asList("1"));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),Arrays.asList("1"));
                    break;
                case NDARRAY:
                    data.put(valueType.name().toLowerCase(),NDArray.create(Nd4j.scalar(1.0)));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),Nd4j.scalar(1.0));
                    break;
                case DOUBLE:
                    data.put(valueType.name().toLowerCase(),1.0);
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),1.0);
                    break;
                case STRING:
                    data.put(valueType.name().toLowerCase(),"1");
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),"1");
                    break;
                case DATA:
                    break;
                case IMAGE:
                    File imageFile = new ClassPathResource("data/5_32x32.png").getFile();
                    Image image = Image.create(imageFile);
                    data.put(valueType.name().toLowerCase(),image);
                    byte[] imageContent = FileUtils.readFileToByteArray(imageFile);
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),imageContent);
                    break;
                case INT64:
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),1);
                    data.put(valueType.name().toLowerCase(),1);
                    break;
                case BYTEBUFFER:
                    data.put(valueType.name().toLowerCase(),ByteBuffer.wrap(new byte[]{1}));
                    assertion.add(valueType.name().toLowerCase(),KonduitPythonUtils.typeForValueType(valueType),new byte[]{1});

                    break;

            }
        }

        PythonConfig config = builder.build();
        PythonVariables createdVariables = KonduitPythonUtils.createPythonVariablesFromDataInput(data,config);
        assertEquals(assertion.size(),createdVariables.size());
        for(int i = 0; i < createdVariables.size(); i++) {
            PythonVariable pythonVariable = createdVariables.get(i);
            PythonVariable assertionVariable = assertion.get(pythonVariable.getName());
            assertNotNull(pythonVariable);
            assertNotNull(assertionVariable);
            if(pythonVariable.getType().equals(PythonTypes.BYTES)) {
                byte[] createdBytes = (byte[]) pythonVariable.getValue();
                byte[] assertionBytes = (byte[]) assertionVariable.getValue();
                assertArrayEquals(assertionBytes,createdBytes);
            }
            else
                assertEquals(assertionVariable.getValue(),pythonVariable.getValue());
        }

    }

    @Test
    public void testListInsert() throws Exception {
        Data data = Data.empty();
        for(ValueType valueType : ValueType.values()) {
            switch(valueType) {
                case STRING:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList("value"),
                            valueType);
                    assertEquals(Arrays.asList("value"),data.getList(valueType.toString(),valueType));
                    break;
                case BYTES:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList("value".getBytes()),
                            valueType);
                    assertArrayEquals("value".getBytes(), (byte[]) data.getList(valueType.toString(),valueType).get(0));
                    break;
                case IMAGE:
                    ClassPathResource classPathResource = new ClassPathResource("data/5_32x32.png");
                    File image = classPathResource.getFile();
                    Image image2 = Image.create(image);
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(image2),
                            valueType);
                    assertEquals(Arrays.asList(image2),data.getList(valueType.toString(),valueType));

                    break;
                case DOUBLE:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(1.0),
                            valueType);
                    assertEquals(Arrays.asList(1.0),data.getList(valueType.toString(),valueType));
                    break;
                case INT64:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(1),
                            valueType);
                    assertEquals(1L,data.getList(valueType.toString(),valueType).get(0));

                    break;
                case BOOLEAN:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(true),
                            valueType);
                    assertEquals(Arrays.asList(true),data.getList(valueType.toString(),valueType));
                    break;
                case BOUNDING_BOX:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(BoundingBox.create(1.0,1.0,1.0,1.0)),
                            valueType);
                    assertEquals(Arrays.asList(BoundingBox.create(1.0,1.0,1.0,1.0)),data.getList(valueType.toString(),valueType));

                    break;
                case POINT:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(Point.create(1,1,1)),
                            valueType);
                    assertEquals(Arrays.asList(Point.create(1,1,1)),data.getList(valueType.toString(),valueType));

                    break;

                case NDARRAY:
                    KonduitPythonUtils.insertListIntoData(
                            data,valueType.toString(),
                            Arrays.asList(Nd4j.scalar(1.0)),
                            valueType);
                    assertEquals(Arrays.asList(new ND4JNDArray(Nd4j.scalar(1.0))),data.getList(valueType.toString(),valueType));

                    break;

                case BYTEBUFFER:
                    KonduitPythonUtils.insertListIntoData(
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
        PythonIO lenOutput = PythonIO.builder()
                .name("len_input")
                .type(ValueType.INT64)
                .pythonType("int")
                .build();
        PythonIO pythonInput = PythonIO.builder()
                .type(ValueType.BYTES)
                .pythonType("bytes")
                .name("input")
                .build();
        PythonConfig pythonConfig = PythonConfig.builder()
                 .ioOutput("input",pythonInput)
                .ioOutput("len_input",lenOutput)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();

        KonduitPythonUtils.addObjectToPythonVariables(pythonVariables,"input",new byte[]{1});
        KonduitPythonUtils.addObjectToPythonVariables(pythonVariables,"len_input",1);


        KonduitPythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        byte[] inputs = input.getBytes("input");
        assertArrayEquals(new byte[]{1},inputs);
    }


    @Test
    public void testInsertImage() throws Exception {
        PythonIO lenOutput = PythonIO.builder()
                .name("len_input")
                .type(ValueType.INT64)
                .pythonType("int")
                .build();
        PythonIO pythonInput = PythonIO.builder()
                .type(ValueType.IMAGE)
                .pythonType("bytes")
                .name("input")
                .build();
        PythonConfig pythonConfig = PythonConfig.builder()
                .ioOutput("input",pythonInput)
                .ioOutput("len_input",lenOutput)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();


        ClassPathResource testImage = new ClassPathResource("data/5_32x32.png");
        File image = testImage.getFile();
        byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(image));
        Image assertionImage = Image.create(image);
        pythonVariables.add("input",PythonTypes.BYTES,imageBytes);
        pythonVariables.add("len_input",PythonTypes.INT,imageBytes.length);

        KonduitPythonUtils.insertBytesIntoPythonVariables(
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
        PythonIO lenOutput = PythonIO.builder()
                .name("len_input")
                .type(ValueType.INT64)
                .pythonType("int")
                .build();
        PythonIO pythonInput = PythonIO.builder()
                .type(ValueType.BYTES)
                .pythonType("bytes")
                .name("input")
                .build();
        PythonConfig pythonConfig = PythonConfig.builder()
                .ioOutput("input",pythonInput)
                .ioOutput("len_input",lenOutput)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();



        pythonVariables.add("input",PythonTypes.BYTES,"input".getBytes());
        pythonVariables.add("len_input",PythonTypes.INT,"input".getBytes().length);

        KonduitPythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        String inputs = new String(input.getBytes("input"));
        assertEquals("input",inputs);
    }

    @Test
    public void testInsertByteBuffer() throws Exception {
        PythonIO lenOutput = PythonIO.builder()
                .name("len_input")
                .type(ValueType.INT64)
                .pythonType("int")
                .build();
        PythonIO pythonInput = PythonIO.builder()
                .type(ValueType.BYTEBUFFER)
                .pythonType("bytes")
                .name("input")
                .build();
        PythonConfig pythonConfig = PythonConfig.builder()
                .ioOutput("input",pythonInput)
                .ioOutput("len_input",lenOutput)
                .build();

        Data input = Data.empty();
        PythonVariables pythonVariables = new PythonVariables();


        pythonVariables.add("input",PythonTypes.BYTES,new byte[]{1});
        pythonVariables.add("len_input",PythonTypes.INT,1);


        KonduitPythonUtils.insertBytesIntoPythonVariables(
                input,
                pythonVariables,
                "input"
                ,pythonConfig);
        ByteBuffer inputs = input.getByteBuffer("input");
        assertEquals(ByteBuffer.wrap(new byte[]{1}),inputs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalInsertList() {
        KonduitPythonUtils.insertListIntoData(
                Data.empty(),"illegal",
                Arrays.asList(Collections.emptyList()),
                ValueType.LIST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalInsertData() {
        KonduitPythonUtils.insertListIntoData(
                Data.empty(),"illegal",
                Arrays.asList(Data.empty()),
                ValueType.DATA);
    }
}
