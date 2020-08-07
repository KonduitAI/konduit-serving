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
package ai.konduit.serving.python;

import ai.konduit.serving.data.image.data.MatImage;
import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.data.image.BImage;
import ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline;
import ai.konduit.serving.python.util.PythonUtils;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.datavec.python.Python;
import org.datavec.python.PythonExecutioner;
import org.datavec.python.PythonType;
import org.datavec.python.PythonVariables;
import org.junit.Test;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.factory.Nd4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.compare;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.junit.Assert.*;

public class PythonRunnerTests {


    @Test
    public void testDictUtilsPoint() {
        Python.deleteNonMainContexts();

        Point point = Point.create(1,2);
        Map<String,Object> convertedDict = DictUtils.toPointDict(point);
        Map<String,Object> assertion = new LinkedHashMap<>();
        assertion.put("x",point.x());
        assertion.put("y",point.y());
        assertion.put("label","");
        assertion.put("dimensions",2);
        assertEquals(assertion,convertedDict);

        Point point2 = Point.create(1,2,3);
        assertion = new LinkedHashMap<>();
        convertedDict = DictUtils.toPointDict(point2);
        assertion.put("x",point2.x());
        assertion.put("y",point2.y());
        assertion.put("z",point2.z());
        assertion.put("label","");
        assertion.put("dimensions",3);
        assertEquals(assertion,convertedDict);

        Point point3 = Point.create(new double[] {1,2},"",0.0);
        assertion = new LinkedHashMap<>();
        convertedDict = DictUtils.toPointDict(point3);
        assertion.put("x",point3.x());
        assertion.put("y",point3.y());
        assertion.put("label","");
        assertion.put("dimensions",2);
        assertEquals(assertion,convertedDict);

    }

    @Test(expected = IllegalStateException.class)
    public void test4dPointFailure() {
        Point point4 = Point.create(new double[]{1,2,3,4},"",0.0);
        fail("Should have failed with an IllegalStateException");
    }

    @Test
    public void testDictUtilsBoundingBox() {
        Python.deleteNonMainContexts();

        BoundingBox boundingBox = BoundingBox.create(0.0,1.0,1.0,1.0);
        Map<String, Object> boundingBoxDict = DictUtils.toBoundingBoxDict(boundingBox);
        Map<String,Object> assertion = new LinkedHashMap<>();
        assertion.put("cx",0.0);
        assertion.put("cy",1.0);
        assertion.put("height",1.0);
        assertion.put("width",1.0);
        assertion.put("label","");
        assertion.put("probability",0.0);
        assertion.put("x1",-0.5);
        assertion.put("x2",0.5);
        assertion.put("y1",0.5);
        assertion.put("y2",1.5);
        assertEquals(assertion,boundingBoxDict);


        assertion = new LinkedHashMap<>();
        BoundingBox boundingBox2 = BoundingBox.create(0.0,1.0,1.0,1.0,"label",0.0);
        boundingBoxDict = DictUtils.toBoundingBoxDict(boundingBox2);
        assertion.put("cx",0.0);
        assertion.put("cy",1.0);
        assertion.put("height",1.0);
        assertion.put("width",1.0);
        assertion.put("label","label");
        assertion.put("probability",0.0);
        assertion.put("x1",-0.5);
        assertion.put("x2",0.5);
        assertion.put("y1",0.5);
        assertion.put("y2",1.5);
        assertEquals(assertion,boundingBoxDict);
    }

    @Test
    public void testAllInputTypes() {
        Python.deleteNonMainContexts();
        PythonType.TypeName[] values = PythonType.TypeName.values();
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        StringBuffer codeBuffer = new StringBuffer();

        Data data = Data.empty();
        Data assertion = Data.empty();
        for(int i = 0; i < values.length; i++) {
            String varName = "i" + i;
            String codeSnippet = varName + " += 1\n";
            switch(values[i]) {
                case FLOAT:
                    builder.pythonInput(String.valueOf(i),values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,1.0f);
                    assertion.put(varName,2.0f);
                    break;
                case INT:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,1);
                    assertion.put(varName,2);
                    break;
                case DICT:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    builder.typeForDictionaryForVariableName(varName,ValueType.POINT);
                    builder.typeForDictionaryForOutputVariableName(varName,ValueType.POINT);
                    codeBuffer.append(varName + "['z'] = 1\n");
                    Point point = Point.create(1, 2);
                    data.put(varName,point);
                    assertion.put(varName,Point.create(1,2,1));
                    break;
                case STR:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(varName + " += '1'\n");
                    data.put(varName,String.valueOf(1));
                    assertion.put(varName,"11");
                    break;
                case NDARRAY:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    codeBuffer.append(codeSnippet);
                    data.put(varName,new ND4JNDArray(Nd4j.scalar(1.0)));
                    assertion.put(varName,new ND4JNDArray(Nd4j.scalar(2.0)));
                    break;
                case BYTES:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    builder.pythonOutput("len_" + varName ,PythonType.TypeName.INT.name());
                    builder.outputTypeByteConversion(varName,ValueType.BYTES);
                    codeBuffer.append(varName + "= bytes(" + varName + "); len_" + varName + " = len(" + varName + ")\n");
                    data.put(varName,new byte[]{1});
                    assertion.put(varName,new byte[]{1});
                    assertion.put("len_" + varName,1);
                    break;
                case LIST:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    builder.listTypeForVariableName(varName, ValueType.INT64);
                    builder.listTypeForOutputVariableName(varName,ValueType.INT64);
                    codeBuffer.append(varName + ".append(1)\n");
                    data.putListInt64(varName,Arrays.asList(1L));
                    assertion.putListInt64(varName, Arrays.asList(1L,1L));
                    break;
                case BOOL:
                    builder.pythonInput(varName,values[i].name());
                    builder.pythonOutput(varName,values[i].name());
                    data.put(varName,true);
                    codeBuffer.append(varName + " = True\n");
                    assertion.put(varName,true);
                    break;
            }
        }

        builder.pythonCode(codeBuffer.toString());

        PythonConfig pythonConfig = builder.build();
        PythonStep pythonStep = new PythonStep()
                .pythonConfig(pythonConfig);
        SequencePipeline sequencePipeline = SequencePipeline.builder()
                .add(pythonStep)
                .build();

        PipelineExecutor executor = sequencePipeline.executor();
        Data exec = executor.exec(data);
        assertEquals(exec.keys().size(),pythonConfig.getPythonOutputs().size());
        assertEquals(assertion,exec);
    }


    @Test
    public void testImageSerde() throws Exception {
        Python.deleteNonMainContexts();
        PythonConfig.PythonConfigBuilder builder = PythonConfig.builder();
        builder.pythonInput("input2", PythonType.TypeName.BYTES.name());
        builder.pythonOutput("len_output2",PythonType.TypeName.INT.name());
        builder.pythonOutput("output2",PythonType.TypeName.BYTES.name());

        builder.pythonCode("output2 = bytes(input2); len_output2 = len(output2)\n");
        builder.outputTypeByteConversion("output2",ValueType.IMAGE);

        PythonConfig pythonConfig = builder.build();
        PythonStep pythonStep = new PythonStep()
                .pythonConfig(pythonConfig);
        SequencePipeline sequencePipeline = SequencePipeline.builder()
                .add(pythonStep)
                .build();
        PipelineExecutor executor = sequencePipeline.executor();

        Data input = Data.empty();
        Data assertion = Data.empty();

        ClassPathResource classPathResource = new ClassPathResource("data/5_32x32.png");
        File image = classPathResource.getFile();
        Image assertionImage = Image.create(image);
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(image));
        assertion.put("output2",bytes);
        input.put("input2", Image.create(image));
        Data exec = executor.exec(input);
        List<String> keys = exec.keys();
        assertTrue(keys.contains("output2"));
        assertTrue(keys.contains("len_output2"));

        Image testImage = exec.getImage("output2");
        assertEquals(assertionImage.height(),testImage.height());
        assertEquals(assertionImage.width(),testImage.width());
    }


    @Test
    public void testMatInput() throws Exception {
        PythonVariables pythonVariables = new PythonVariables();
        ClassPathResource classPathResource = new ClassPathResource("data/5_32x32.png");
        File image = classPathResource.getFile();
        Mat mat = imread(image.getAbsolutePath());
        MatImage matImage = new MatImage(mat);

        PythonUtils.addImageToPython(pythonVariables,"image",matImage);
        assertNotNull(pythonVariables.getBytesValue("image"));

        BytePointer image1 = pythonVariables.getBytesValue("image");
        Mat mat2 = new Mat(mat.rows(),mat.cols(),mat.type(),image1,mat.step());
        assertEquals(mat.data(),mat2.data());
        assertEquals(mat.rows(),mat2.rows());
        assertEquals(mat.cols(),mat2.cols());
        assertEquals(mat.step(),mat2.step());
    }

}
