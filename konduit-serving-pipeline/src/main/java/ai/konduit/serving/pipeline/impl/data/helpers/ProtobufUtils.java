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
package ai.konduit.serving.pipeline.impl.data.helpers;

import ai.konduit.serving.pipeline.api.data.*;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.Value;

import java.nio.ByteBuffer;
import java.util.*;

import ai.konduit.serving.pipeline.impl.data.image.Png;
import ai.konduit.serving.pipeline.impl.data.ndarray.SerializedNDArray;
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage;
import ai.konduit.serving.pipeline.impl.data.wrappers.ListValue;
import com.google.protobuf.ByteString;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;

public class ProtobufUtils {

    public static DataProtoMessage.DataMap serialize(Map<String,Value> dataMap) {

        Map<String, DataProtoMessage.DataScheme> pbItemsMap = new HashMap<>();
        Iterator<Map.Entry<String, Value>> iterator = dataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            DataProtoMessage.DataScheme item = null;

            if (value.type() == ValueType.STRING) {
                item = DataProtoMessage.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type() == ValueType.BOOLEAN) {
                item = DataProtoMessage.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type() == ValueType.INT64) {
                item = DataProtoMessage.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            } else if (value.type() == ValueType.DOUBLE) {
                item = DataProtoMessage.DataScheme.newBuilder().
                        setDoubleValue((Double) nextItem.getValue().get()).
                        setTypeValue(ValueType.DOUBLE.ordinal()).
                        build();
            }
            else if (value.type() == ValueType.IMAGE) {
                Image image = (Image) nextItem.getValue().get();
                Png p = image.getAs(Png.class);
                byte[] imageBytes = p.getBytes();
                //byte[] imageBytes = new JavaImageConverters.IdentityConverter().convert(image, byte[].class);
                DataProtoMessage.Image pbImage = DataProtoMessage.Image.newBuilder().
                        addData(ByteString.copyFrom(imageBytes)).
                        build();

                item = DataProtoMessage.DataScheme.newBuilder().
                        setImValue(pbImage).
                        setTypeValue(ValueType.IMAGE.ordinal()).
                        build();
            }
            else if (value.type() == ValueType.NDARRAY) {
                NDArray ndArray = (NDArray)nextItem.getValue().get();
                SerializedNDArray sn = ndArray.getAs(SerializedNDArray.class);

                byte[] bufferBytes = new byte[sn.getBuffer().remaining()];
                sn.getBuffer().get(bufferBytes);
                ByteString byteString = ByteString.copyFrom(bufferBytes);
                List<ByteString> byteStringList = new ArrayList<>();
                byteStringList.add(byteString);

                // TODO: setType must be fixed when anoter ndarray types are supported.
                DataProtoMessage.NDArray pbNDArray = DataProtoMessage.NDArray.newBuilder().
                        addAllShape(Arrays.asList(ArrayUtils.toObject(sn.getShape()))).
                        addAllArray(byteStringList).
                        setType(DataProtoMessage.NDArray.ValueType.FLOAT).
                        build();

                item = DataProtoMessage.DataScheme.newBuilder().
                        setNdValue(pbNDArray).
                        setTypeValue(ValueType.NDARRAY.ordinal()).
                        build();
            }
            else if (value.type() == ValueType.DATA) {
                JData jData = (JData)nextItem.getValue().get();
                DataProtoMessage.DataMap dataMapEmbedded = serialize(jData.getDataMap());

                item = DataProtoMessage.DataScheme.newBuilder().
                        setMetaData(dataMapEmbedded).
                        setTypeValue(ValueType.DATA.ordinal()).
                        build();
            }
            else if (value.type() == ValueType.LIST) {
                ListValue lv = (ListValue)value;
                if (lv.elementType() == ValueType.INT64) {
                    List<Long> longs = (List<Long>)nextItem.getValue().get();
                    DataProtoMessage.Int64List toAdd = DataProtoMessage.Int64List.newBuilder().addAllList(longs).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setIList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.INT64.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
                if (lv.elementType() == ValueType.BOOLEAN) {
                    List<Boolean> longs = (List<Boolean>)nextItem.getValue().get();
                    DataProtoMessage.BooleanList toAdd = DataProtoMessage.BooleanList.newBuilder().addAllList(longs).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setBList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.BOOLEAN.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
                if (lv.elementType() == ValueType.DOUBLE) {
                    List<Double> doubles = (List<Double>)nextItem.getValue().get();
                    DataProtoMessage.DoubleList toAdd = DataProtoMessage.DoubleList.newBuilder().addAllList(doubles).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setDList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.DOUBLE.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
                if (lv.elementType() == ValueType.STRING) {
                    List<String> strings = (List<String>)nextItem.getValue().get();
                    DataProtoMessage.StringList toAdd = DataProtoMessage.StringList.newBuilder().addAllList(strings).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setSList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            //setListValue(toAddGen).
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.STRING.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
                if (lv.elementType() == ValueType.IMAGE) {
                    List<Image> images = (List<Image>)nextItem.getValue().get();
                    List<DataProtoMessage.Image> pbImages = new ArrayList<>();
                    for (val image : images) {
                        Png p = image.getAs(Png.class);
                        byte[] imageBytes = p.getBytes();
                        DataProtoMessage.Image pbImage = DataProtoMessage.Image.newBuilder().
                                addData(ByteString.copyFrom(imageBytes)).
                                build();
                        pbImages.add(pbImage);
                    }

                    DataProtoMessage.ImageList toAdd = DataProtoMessage.ImageList.newBuilder().addAllList(pbImages).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setImList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.IMAGE.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
                if (lv.elementType() == ValueType.NDARRAY) {
                    List<NDArray> arrays = (List<NDArray>)nextItem.getValue().get();
                    List<DataProtoMessage.NDArray> pbArrays = new ArrayList<>();
                    for (val arr : arrays) {
                        SerializedNDArray sn = arr.getAs(SerializedNDArray.class);
                        byte[] bufferBytes = new byte[sn.getBuffer().remaining()];
                        sn.getBuffer().get(bufferBytes);
                        ByteString byteString = ByteString.copyFrom(bufferBytes);
                        List<ByteString> byteStringList = new ArrayList<>();
                        byteStringList.add(byteString);
                        DataProtoMessage.NDArray pbNDArray = DataProtoMessage.NDArray.newBuilder().
                                addAllShape(Arrays.asList(ArrayUtils.toObject(sn.getShape()))).
                                addAllArray(byteStringList).
                                setType(DataProtoMessage.NDArray.ValueType.FLOAT).
                                build();
                        pbArrays.add(pbNDArray);
                    }
                    DataProtoMessage.NDArrayList toAdd = DataProtoMessage.NDArrayList.newBuilder().addAllList(pbArrays).build();
                    DataProtoMessage.List toAddGen = DataProtoMessage.List.newBuilder().setNdList(toAdd).build();
                    item = DataProtoMessage.DataScheme.newBuilder().
                            setListValue(toAddGen).
                            setListTypeValue(ValueType.NDARRAY.ordinal()).
                            setTypeValue(ValueType.LIST.ordinal()).
                            build();
                }
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            pbItemsMap.put(nextItem.getKey(), item);
        }
        DataProtoMessage.DataMap pbDataMap = DataProtoMessage.DataMap.newBuilder().
                putAllMapItems(pbItemsMap).
                build();
        return pbDataMap;
    }

    public static Data deserialize(DataProtoMessage.DataMap dataMap) {
        JData retData = new JData();
        Map<String, DataProtoMessage.DataScheme> schemeMap = dataMap.getMapItemsMap();
        Iterator<Map.Entry<String, DataProtoMessage.DataScheme>> iterator =
                schemeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DataProtoMessage.DataScheme> entry = iterator.next();
            DataProtoMessage.DataScheme item = entry.getValue();
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.STRING.ordinal()) {
                retData.put(entry.getKey(), item.getSValue());
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.BOOLEAN.ordinal()) {
                retData.put(entry.getKey(), item.getBoolValue());
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.INT64.ordinal()) {
                retData.put(entry.getKey(), item.getIValue());
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.DOUBLE.ordinal()) {
                retData.put(entry.getKey(), item.getDoubleValue());
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.DATA.ordinal()) {
                DataProtoMessage.DataMap itemMetaData = item.getMetaData();
                Data embeddedData = deserialize(itemMetaData);
                retData.put(entry.getKey(), embeddedData);
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.LIST.ordinal()) {
                if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.DOUBLE.ordinal()) {
                    retData.putListDouble(entry.getKey(), item.getListValue().getDList().getListList());
                }
                else if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.BOOLEAN.ordinal()) {
                    retData.putListBoolean(entry.getKey(), item.getListValue().getBList().getListList());
                }
                if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.INT64.ordinal()) {
                    retData.putListInt64(entry.getKey(), item.getListValue().getIList().getListList());
                }
                if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.STRING.ordinal()) {
                    retData.putListString(entry.getKey(), item.getListValue().getSList().getListList());
                }
                if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.IMAGE.ordinal()) {
                    List<DataProtoMessage.Image> pbImages = item.getListValue().getImList().getListList();
                    List<Image> images = new ArrayList<>();
                    for (val pbImage : pbImages) {
                        List<ByteString> pbData = pbImage.getDataList();
                        byte[] data = pbData.get(0).toByteArray();
                        // TODO: obviously should be working for different formats
                        Png png = new Png(data);
                        images.add(Image.create(png));
                    }
                    retData.putListImage(entry.getKey(), images);
                }
                if (item.getListTypeValue() == DataProtoMessage.DataScheme.ValueType.NDARRAY.ordinal()) {
                    List<DataProtoMessage.NDArray> pbArrays = item.getListValue().getNdList().getListList();
                    List<NDArray> arrays = new ArrayList<>();
                    for (val pbArray : pbArrays) {
                        List<Long> shapes = pbArray.getShapeList();
                        long[] aShapes = new long[shapes.size()];
                        for (int i = 0; i < shapes.size(); ++i) {
                            aShapes[i] = shapes.get(i);
                        }

                        List<ByteString> data = pbArray.getArrayList();
                        DataProtoMessage.NDArray.ValueType type = pbArray.getType();
                        byte[] bytes = data.get(0).toByteArray();
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        // TODO: fix ndarray type
                        SerializedNDArray ndArray = new SerializedNDArray(NDArrayType.FLOAT, aShapes, bb);
                        arrays.add(NDArray.create(ndArray));
                    }
                    retData.putListNDArray(entry.getKey(), arrays);
                }
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.IMAGE.ordinal()) {
                DataProtoMessage.Image pbImage = item.getImValue();
                List<ByteString> pbData = pbImage.getDataList();
                byte[] data = pbData.get(0).toByteArray();
                // TODO: obviously should be working for different formats
                Png png = new Png(data);
                retData.put(entry.getKey(), Image.create(png));
            }
            if (item.getTypeValue() == DataProtoMessage.DataScheme.ValueType.NDARRAY.ordinal()) {
                DataProtoMessage.NDArray pbArray = item.getNdValue();
                List<Long> shapes = pbArray.getShapeList();
                long[] aShapes = new long[shapes.size()];
                for (int i = 0; i < shapes.size(); ++i) {
                    aShapes[i] = shapes.get(i);
                }

                List<ByteString> data = pbArray.getArrayList();
                DataProtoMessage.NDArray.ValueType type = pbArray.getType();
                byte[] bytes = data.get(0).toByteArray();
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                // TODO: fix ndarray type
                SerializedNDArray ndArray = new SerializedNDArray(NDArrayType.FLOAT, aShapes, bb);
                retData.put(entry.getKey(), NDArray.create(ndArray));
            }
        }
        return retData;
    }

}
