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

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.data.image.data.FrameImage;
import ai.konduit.serving.data.image.data.MatImage;
import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import ai.konduit.serving.pipeline.impl.data.image.*;
import lombok.SneakyThrows;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;
import org.datavec.python.PythonContextManager;
import org.datavec.python.PythonJob;
import org.datavec.python.PythonType;
import org.datavec.python.PythonVariables;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@CanRun(PythonStep.class)
public class PythonRunner implements PipelineStepRunner {

    private PythonStep pythonStep;
    private PythonJob pythonJob;

    @SneakyThrows
    public PythonRunner(PythonStep pythonStep) {
        this.pythonStep = pythonStep;
        pythonJob = PythonJob.builder()
                .setupRunMode(pythonStep.pythonConfig().isSetupAndRun())
                .code(pythonStep.pythonConfig().getPythonCode())
                .build();
    }


    @Override
    public void close() {

    }

    @Override
    public PipelineStep getPipelineStep() {
        return pythonStep;
    }

    @SneakyThrows
    @Override
    public Data exec(Context ctx, Data data) {
        PythonContextManager.deleteNonMainContexts();
        PythonVariables pythonVariables = new PythonVariables();
        Data ret = Data.empty();
        for(String key : data.keys()) {
            switch(data.type(key)) {
                case NDARRAY:
                    NDArray ndArray = data.getNDArray(key);
                    INDArray arr = ndArray.getAs(INDArray.class);
                    pythonVariables.addNDArray(key,arr);
                    break;
                case BYTES:
                    byte[] bytes = data.getBytes(key);
                    BytePointer bytePointer = new BytePointer(bytes);
                    pythonVariables.addBytes(key,bytePointer);
                    break;
                case DOUBLE:
                    double aDouble = data.getDouble(key);
                    pythonVariables.addFloat(key,aDouble);
                    break;
                case LIST:
                    throw new IllegalArgumentException("Illegal type " + data.type(key));
                case INT64:
                    long aLong = data.getLong(key);
                    pythonVariables.addInt(key,aLong);
                    break;
                case BOOLEAN:
                    boolean aBoolean = data.getBoolean(key);
                    pythonVariables.addBool(key,aBoolean);
                    break;
                case STRING:
                    String string = data.getString(key);
                    pythonVariables.addStr(key,string);
                    break;
                case IMAGE:
                    Image image = data.getImage(key);
                    if(image instanceof BImage) {
                        BImage bImage = (BImage) image;
                        BufferedImage bufferedImage = bImage.getAs(BufferedImage.class);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage,"jpg",byteArrayOutputStream);
                    }
                    else if(image instanceof PngImage) {
                        PngImage pngImage = (PngImage) image;
                        Png png = pngImage.getAs(Png.class);
                        ret.put(key,png.getBytes());
                    }
                    else if(image instanceof GifImage) {
                        GifImage gifImage = (GifImage) image;
                        Gif gif = gifImage.getAs(Gif.class);
                        ret.put(key,gif.getBytes());
                    }
                    else if(image instanceof JpegImage) {
                        JpegImage jpegImage = (JpegImage) image;
                        Jpeg jpeg = jpegImage.getAs(Jpeg.class);
                        ret.put(key,jpeg.getBytes());
                    }
                    else if(image instanceof BmpImage) {
                        BmpImage bmpImage = (BmpImage) image;
                        Bmp bmp = bmpImage.getAs(Bmp.class);
                        ret.put(key,bmp.getBytes());
                    }
                    else if(image instanceof FrameImage) {
                        FrameImage frameImage = (FrameImage) image;
                        Frame frame = frameImage.getAs(Frame.class);
                        ret.put(key,frameImage);
                    }
                    else if(image instanceof MatImage) {
                        MatImage matImage = (MatImage) image;
                        Mat as = matImage.getAs(Mat.class);
                        ret.put(key,matImage);

                    }

                    break;
                case BOUNDING_BOX:
                    BoundingBox boundingBox = data.getBoundingBox(key);
                    Map<String,Object> boundingBoxValues = new LinkedHashMap<>();
                    boundingBoxValues.put("cx",boundingBox.cx());
                    boundingBoxValues.put("cy",boundingBox.cy());
                    boundingBoxValues.put("width",boundingBox.width());
                    boundingBoxValues.put("height",boundingBox.height());
                    boundingBoxValues.put("label",boundingBox.label());
                    boundingBoxValues.put("probability",boundingBox.probability());
                    boundingBoxValues.put("cy",boundingBox.cy());
                    boundingBoxValues.put("x1",boundingBox.x1());
                    boundingBoxValues.put("x2",boundingBox.x2());
                    boundingBoxValues.put("y1",boundingBox.y1());
                    boundingBoxValues.put("y2",boundingBox.y2());
                    pythonVariables.addDict(key,boundingBoxValues);
                    break;
                case POINT:
                    Map<String,Object> pointerValue = new LinkedHashMap<>();
                    Point point = data.getPoint(key);
                    pointerValue.put("x",point.x());
                    pointerValue.put("y",point.y());
                    pointerValue.put("z",point.z());
                    pointerValue.put("dimensions",point.dimensions());
                    pointerValue.put("label",point.label());
                    pointerValue.put("probability", point.probability());
                    pythonVariables.addDict(key,pointerValue);
                    break;
                case DATA:
                    throw new IllegalArgumentException("Illegal type " + data.type(key));

            }
        }

        PythonVariables outputs = new PythonVariables();
        Map<String, String> pythonOutputs = pythonStep.pythonConfig().getPythonOutputs();
        for(Map.Entry<String,String> entry : pythonOutputs.entrySet()) {
            outputs.add(entry.getKey(), PythonType.valueOf(entry.getValue()));
        }

        pythonJob.exec(pythonVariables,outputs);
        for(String variable : outputs.getVariables()) {
            switch(outputs.getType(variable).getName()) {
                case BOOL:
                    ret.put(variable,outputs.getBooleanValue(variable));
                    break;
                case LIST:
                    throw new IllegalArgumentException("Illegal output type " + outputs.getType(variable).getName());
                case BYTES:
                    ret.put(variable,outputs.getBytesValue(variable).getStringBytes());
                    break;
                case NDARRAY:
                    ret.put(variable,new ND4JNDArray(outputs.getNDArrayValue(variable)));
                    break;
                case STR:
                    ret.put(variable,outputs.getStrValue(variable));
                    break;
                case DICT:
                    break;
                case INT:
                    ret.put(variable,outputs.getIntValue(variable));
                    break;
                case FLOAT:
                    ret.put(variable,outputs.getFloatValue(variable));
                    break;

            }
        }

        return ret;
    }
}
