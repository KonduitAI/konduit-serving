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
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(key),"No input type specified for list with key " + key);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(key);
                    List<Object> list = data.getList(key, valueType);
                    pythonVariables.addList(key,list.toArray(new Object[list.size()]));
                    break;
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
                        ret.put(key,frameImage);
                    }
                    else if(image instanceof MatImage) {
                        MatImage matImage = (MatImage) image;
                        ret.put(key,matImage);
                    }

                    break;
                case BOUNDING_BOX:
                    BoundingBox boundingBox = data.getBoundingBox(key);
                    Map<String,Object> boundingBoxValues = DictUtils.toBoundingBoxDict(boundingBox);
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
                    Preconditions.checkState(pythonStep.pythonConfig().getListTypesForVariableName().containsKey(variable),"No input type specified for list with key " + variable);
                    List listValue = outputs.getListValue(variable);
                    ValueType valueType = pythonStep.pythonConfig().getListTypesForVariableName().get(variable);
                    switch(valueType) {
                        case IMAGE:
                            /**
                             * TODO: LIKELY DOES NOT WORK. NEED TO LOOK IN TO IMAGE FACTORIES.
                             */
                            List<Image> images = new ArrayList<>(listValue.size());
                            for(Object  o : listValue) {
                                Image image = Image.create(o);
                                images.add(image);
                            }
                            ret.putListImage(variable,images);
                            break;
                        case DOUBLE:
                            List<Double> doubles = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                Number number = (Number) o;
                                doubles.add(number.doubleValue());
                            }
                            ret.putListDouble(variable,doubles);
                            break;
                        case INT64:
                            List<Long> longs = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                Number number = (Number) o;
                                longs.add(number.longValue());
                            }
                            ret.putListInt64(variable,longs);
                            break;
                        case BOOLEAN:
                            List<Boolean> booleans = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                Boolean b = (Boolean) o;
                                booleans.add(b);
                            }
                            ret.putListBoolean(variable,booleans);
                            break;
                        case BOUNDING_BOX:
                            List<BoundingBox> boundingBoxes = new ArrayList<>(listValue.size());
                            for(Object input : listValue) {
                                Map<String,Object> dict = (Map<String,Object>) input;
                                BoundingBox boundingBox = DictUtils.boundingBoxFromDict(dict);
                                boundingBoxes.add(boundingBox);
                            }
                            ret.putListBoundingBox(variable,boundingBoxes);
                            break;
                        case STRING:
                            List<String> strings = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                strings.add(o.toString());
                            }
                            break;
                        case POINT:
                            List<Point> points = new ArrayList<>();
                            for(Object o : listValue) {
                                Map<String,Object> dict = (Map<String,Object>) o;
                                points.add(DictUtils.fromPointDict(dict));
                            }
                            ret.putListPoint(variable,points);
                            break;
                        case DATA:
                            throw new IllegalArgumentException("Unable to de serialize dat from python");
                        case NDARRAY:
                            List<INDArray> ndArrays = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                INDArray arr = (INDArray) o;
                                ndArrays.add(arr);
                            }
                            break;
                        case BYTES:
                            List<byte[]> bytes = new ArrayList<>(listValue.size());
                            for(Object o : listValue) {
                                byte[] arr = (byte[]) o;
                                bytes.add(arr);
                            }
                            ret.putListBytes(variable,bytes);
                            break;
                        case LIST:
                            throw new IllegalArgumentException("List of lists not allowed");
                    }
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
                    ValueType dictValueType = pythonStep.pythonConfig().getTypeForDictionaryForOutputVariableNames().get(variable);
                    Map<String,Object> items = (Map<String, Object>) outputs.getDictValue(variable);
                    /**
                     * TODO: Figure out how to handle attributes
                     */
                    throw new IllegalArgumentException("Unable to handle dictionary attributes");
      /*           switch(dictValueType) {
                     case BYTES:
                         break;
                     case LIST:
                         break;
                     case NDARRAY:
                         break;
                     case DATA:
                         break;
                     case POINT:
                         break;
                     case STRING:
                         break;
                     case BOUNDING_BOX:
                          break;
                     case BOOLEAN:
                         break;
                     case INT64:
                         break;
                     case DOUBLE:
                         break;
                     case IMAGE:
                         break;
                 }
                    break;*/
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
