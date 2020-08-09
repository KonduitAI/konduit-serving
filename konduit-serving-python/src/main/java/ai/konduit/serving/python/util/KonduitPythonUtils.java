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

import ai.konduit.serving.data.image.data.FrameImage;
import ai.konduit.serving.data.image.data.MatImage;
import ai.konduit.serving.data.nd4j.data.ND4JNDArray;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.image.*;
import ai.konduit.serving.python.DictUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;

import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.python4j.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KonduitPythonUtils {

    public final static    String[] PYTHON_VARIABLE_TYPES = {
            "bool",
            "list",
            "bytes",
            "numpy.ndarray",
            "str",
            "dict",
            "int",
            "float"
    };

    private KonduitPythonUtils() {}


    /**
     * Return an equivalent {@link PythonType}
     * for the given java class. Accepted clases right now are:
     * {@link INDArray}: {@link NumpyArray}
     * double, {@link Double}: {@link PythonTypes#FLOAT}
     * int, {@link Integer}, long {@link Long}: {@link PythonTypes#INT}
     * {@link Map}: {@link PythonTypes#DICT}
     * {@link List}: {@link PythonTypes#LIST}
     * {@link String} : {@link PythonTypes#STR}
     * {@link ByteBuffer}, byte[], : {@link PythonTypes#BYTES}
     * {@link Boolean}, boolean: {@link PythonTypes#BOOL}
     * @param clazz the input class
     * @param <T> the type of the class
     * @return the equivalent {@link PythonType} listed above
     */
    public static <T>  PythonType pythonTypeFor(Class<T> clazz) {
        if(clazz.equals(INDArray.class)) {
            return NumpyArray.INSTANCE;
        } else if(clazz.equals(Float.class)
                || clazz.equals(float.class)
                || clazz.equals(double.class)
                || clazz.equals(Double.class)) {
            return PythonTypes.FLOAT;
        }
        else if(clazz.equals(Integer.class)
                || clazz.equals(int.class)
                || clazz.equals(long.class)
                || clazz.equals(Long.class)) {
            return PythonTypes.INT;
        } else if(clazz.isAssignableFrom(Map.class)) {
            return PythonTypes.DICT;
        } else if(clazz.isAssignableFrom(List.class)) {
            return PythonTypes.LIST;
        } else if(clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return PythonTypes.BOOL;
            //clazz is assignable from doesn't seem to work with direct byte buffer
        } else if(clazz.equals(byte[].class) || clazz.isAssignableFrom(ByteBuffer.class) || clazz.getName().contains("Buffer")) {
            return PythonTypes.BYTES;
        } else if(clazz.isAssignableFrom(CharSequence.class)) {
            return PythonTypes.STR;
        } else {
            throw new IllegalArgumentException("Illegal clazz type " + clazz.getName());
        }
    }

    /**
     * Invoke {@link PythonVariables#add(String, PythonType, Object)}
     * with the given input inferring the {@link PythonType}
     *  based on {@link #pythonTypeFor(Class)} based on the
     *  input targetType
     * @param addTo the variables object to add to
     * @param key the variable name
     * @param input the input object to add
     */
    public static void  addObjectToPythonVariables(PythonVariables addTo,String key,Object input) {
        addTo.add(key,pythonTypeFor(input.getClass()),input);
    }



    /**
     * Get the desired variable
     * with the desired type
     * @param getFrom the variables to get
     * {@link PythonVariables#get(String)} from
     * @param variableName the name of the variable
     * @param clazz the type of the class
     * @param <T> the type
     * @return the type
     */
    public static <T> T getWithType(PythonVariables getFrom,String variableName,Class<T> clazz) {
        PythonVariable pythonVariable = getFrom.get(variableName);
        Object value = pythonVariable.getValue();
        return clazz.cast(value);
    }

    /**
     * Adds an image to a set of python variables.
     * Also adds the length as key_len
     * @param pythonVariables
     * @param key
     * @param image
     * @throws Exception
     */
    public static void addImageToPython(PythonVariables pythonVariables, String key, Image image) throws Exception {
        if(image instanceof BImage) {
            BImage bImage = (BImage) image;
            BufferedImage bufferedImage = bImage.getAs(BufferedImage.class);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage,"jpg",byteArrayOutputStream);
            addObjectToPythonVariables(pythonVariables,key,byteArrayOutputStream.toByteArray());

        }
        else if(image instanceof PngImage) {
            PngImage pngImage = (PngImage) image;
            Png png = pngImage.getAs(Png.class);
            addObjectToPythonVariables(pythonVariables,key,png.getBytes());
        }
        else if(image instanceof GifImage) {
            GifImage gifImage = (GifImage) image;
            Gif gif = gifImage.getAs(Gif.class);
            addObjectToPythonVariables(pythonVariables,key,gif.getBytes());
        }
        else if(image instanceof JpegImage) {
            JpegImage jpegImage = (JpegImage) image;
            Jpeg jpeg = jpegImage.getAs(Jpeg.class);
            addObjectToPythonVariables(pythonVariables,key,jpeg.getBytes());
        }
        else if(image instanceof BmpImage) {
            BmpImage bmpImage = (BmpImage) image;
            Bmp bmp = bmpImage.getAs(Bmp.class);
            addObjectToPythonVariables(pythonVariables,key,bmp.getBytes());
        }
        else if(image instanceof FrameImage) {
            FrameImage frameImage = (FrameImage) image;
            Frame frame = frameImage.getAs(Frame.class);
            addObjectToPythonVariables(pythonVariables,key,frame.data);
        }
        else if(image instanceof MatImage) {
            MatImage matImage = (MatImage) image;
            Mat mat = matImage.getAs(Mat.class);
            int totalLen = (int) mat.elemSize() * mat.cols() * mat.depth() * mat.rows();
            ByteBuffer byteBuffer = mat.data().asByteBuffer();
            byte[] convert = new byte[totalLen];
            byteBuffer.get(convert.length);
            addObjectToPythonVariables(pythonVariables,key,convert);
        }
    }

    /**
     * Insert a list in to the given {@link Data}
     * object. The given list will typically come from a
     * {@link PythonVariables#get(String)} (String)}
     * invocation with the restriction of a single type
     * per list to play well with the konduit serving
     * {@link Data} serialization
     * @param ret the data to insert
     * @param variable the variable used as the key for
     *                 inserting in to the data object
     * @param listValue the list of values to insert
     * @param valueType the value type used for the list
     */
    public static void insertListIntoData(Data ret, String variable, List listValue, ValueType valueType) {
        switch(valueType) {
            case BYTEBUFFER:
                List<ByteBuffer> byteBuffers = new ArrayList<>(listValue.size());
                for(Object  o : listValue) {
                    ByteBuffer byteBuffer = (ByteBuffer) o;
                    byteBuffers.add(byteBuffer);
                }
                ret.putListByteBuffer(variable,byteBuffers);
                break;

            case IMAGE:
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
                    if(input instanceof BoundingBox) {
                        BoundingBox boundingBox = (BoundingBox) input;
                        boundingBoxes.add(boundingBox);
                    }
                    else if(input instanceof Map) {
                        Map<String,Object> dict = DictUtils.toBoundingBoxDict((BoundingBox) input);
                        BoundingBox boundingBox = DictUtils.boundingBoxFromDict(dict);
                        boundingBoxes.add(boundingBox);

                    }

                }

                ret.putListBoundingBox(variable,boundingBoxes);
                break;
            case STRING:
                List<String> strings = new ArrayList<>(listValue.size());
                for(Object o : listValue) {
                    strings.add(o.toString());
                }
                ret.putListString(variable,strings);
                break;
            case POINT:
                List<Point> points = new ArrayList<>();
                for(Object o : listValue) {
                    if(o instanceof Point) {
                        Point point = (Point) o;
                        points.add(point);
                    }
                    else if(o instanceof Map) {
                        Map<String,Object> dict = DictUtils.toPointDict((Point) o);
                        points.add(DictUtils.fromPointDict(dict));
                    }
                }
                ret.putListPoint(variable,points);
                break;
            case DATA:
                throw new IllegalArgumentException("Unable to de serialize dat from python");
            case NDARRAY:
                List<NDArray> ndArrays = new ArrayList<>(listValue.size());
                for(Object o : listValue) {
                    INDArray arr = (INDArray) o;
                    ndArrays.add(new ND4JNDArray(arr));
                }
                ret.putListNDArray(variable,ndArrays);
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
    }

    /**
     * Insert a bytes object in to the givnen {@link Data}
     * object
     * @param ret the data object to insert in to
     * @param outputs the outputs variable to insert in to
     * @param variable the variable representing the key of the value to insert
     * @param pythonConfig the python configuration for validation
     * @throws IOException
     */
    public static void insertBytesIntoPythonVariables(Data ret, PythonVariables outputs, String variable, PythonConfig pythonConfig) throws IOException {
        Preconditions.checkState(pythonConfig.getOutputTypeByteConversions().containsKey(variable),"No output type conversion found for " + variable + " please ensure a type exists for converting bytes to an appropriate data type.");
        ValueType byteOutputValueType = pythonConfig.getOutputTypeByteConversions().get(variable);
        Preconditions.checkState(outputs.get("len_" + variable) != null,"Please ensure a len_" + variable + " is defined for your python script output to get a consistent length from python.");
        Long length = getWithType(outputs,"len_" + variable,Long.class);
        Preconditions.checkNotNull("No byte pointer length found for variable",variable);

        BytePointer bytesValue = new BytePointer(getWithType(outputs,variable,byte[].class));
        Preconditions.checkNotNull("No byte pointer found for variable",variable);
        //ensure length matches what's found in python
        Long capacity = length;
        bytesValue.capacity(capacity);
        switch(byteOutputValueType) {
            case IMAGE:
                ByteBuffer byteBuffer1 = bytesValue.asBuffer();

                if(byteBuffer1.hasArray()) {
                    byte[] bytesContent = byteBuffer1.array();
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytesContent);
                    BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
                    ret.put(variable, Image.create(bufferedImage));
                }
                else {
                    byte[] bytes = new byte[capacity.intValue()];
                    byteBuffer1.get(bytes);
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                    BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
                    Preconditions.checkNotNull(bufferedImage,"Buffered image was not returned. Invalid image bytes passed in.");
                    ret.put(variable,Image.create(bufferedImage));
                }
                break;
            case BYTES:
                ByteBuffer byteBuffer = bytesValue.asByteBuffer();
                byte[] bytes = new byte[byteBuffer.capacity()];
                byteBuffer.get(bytes);
                ret.put(variable,bytes);
                break;
            case BYTEBUFFER:
                ByteBuffer byteBuffer2 = bytesValue.asByteBuffer();
                ret.put(variable,byteBuffer2);
                break;
            case STRING:
                ret.put(variable,bytesValue.getStringBytes());
                break;
            default:
                throw new IllegalArgumentException("Illegal type found for output type conversion  " + byteOutputValueType);
        }
    }
}
