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
package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.data.NDArray;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProtoData extends JData {

    @Override
    public void save(File toFile) throws IOException {
        write(new FileOutputStream(toFile));
    }

    @Override
    public void write(OutputStream toStream) throws IOException {

        Map<String, generated.Data.DataScheme> newItemsMap = javaMapToPbMap();

        generated.Data.DataMap pbDataMap = generated.Data.DataMap.newBuilder().
                putAllMapItems(newItemsMap).
                build();
        pbDataMap.writeTo(toStream);
    }

    public static Data fromFile(File fromFile) throws IOException {
        generated.Data.DataMap.Builder builder = generated.Data.DataMap.newBuilder().mergeFrom(new FileInputStream(fromFile));
        generated.Data.DataMap dataMap = builder.build();
        return pbMapToJavaData(dataMap);
    }

    public static Data fromStream(InputStream stream) throws IOException {
        generated.Data.DataMap.Builder builder = generated.Data.DataMap.newBuilder().mergeFrom(stream);
        generated.Data.DataMap dataMap = builder.build();
        return pbMapToJavaData(dataMap);
    }

    private static Data pbMapToJavaData(generated.Data.DataMap dataMap) {
        JData retData = new JData();
        Map<String, generated.Data.DataScheme> schemeMap = dataMap.getMapItemsMap();
        Iterator<Map.Entry<String, generated.Data.DataScheme>> iterator =
                schemeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, generated.Data.DataScheme> entry = iterator.next();
            generated.Data.DataScheme item = entry.getValue();
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.STRING.ordinal()) {
                retData.put(entry.getKey(), item.getSValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.BOOLEAN.ordinal()) {
                retData.put(entry.getKey(), item.getBoolValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.INT64.ordinal()) {
                retData.put(entry.getKey(), item.getIValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.DOUBLE.ordinal()) {
                retData.put(entry.getKey(), item.getDoubleValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.LIST.ordinal()) {
                // TODO
            }
        }
        return retData;
    }

    private Map<String, generated.Data.DataScheme> javaMapToPbMap() {
        Map<String, generated.Data.DataScheme> newItemsMap = Collections.emptyMap();
        Iterator<Map.Entry<String, Value>> iterator = getDataMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            generated.Data.DataScheme item = null;

            if (value.type().equals(ValueType.STRING)) {
                item = generated.Data.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.BOOLEAN)) {
                item = generated.Data.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.INT64)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.DOUBLE)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.DOUBLE.ordinal()).
                        build();
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            newItemsMap.put(nextItem.getKey(), item);
        }
        return newItemsMap;
    }

    private generated.Data.DataMap javaObjectToPbMessage() {
        Map<String, generated.Data.DataScheme> pbItemsMap = Collections.emptyMap();
        Iterator<Map.Entry<String, Value>> iterator = getDataMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            generated.Data.DataScheme item = null;

            if (value.type().equals(ValueType.STRING)) {
                item = generated.Data.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.BOOLEAN)) {
                item = generated.Data.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.INT64)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.DOUBLE)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.DOUBLE.ordinal()).
                        build();
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            pbItemsMap.put(nextItem.getKey(), item);
        }
        generated.Data.DataMap pbDataMap = generated.Data.DataMap.newBuilder().
                putAllMapItems(pbItemsMap).
                build();
        return pbDataMap;
    }

    public byte[] asBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(baos);
        } catch (IOException e) {
            log.error("Failed write to ByteArrayOutputStream", e);
        }
        return baos.toByteArray();
    }

    public static Data fromBytes(byte[] input) {
        Data retVal = empty();
        generated.Data.DataMap.Builder builder = null;
        try {
            builder = generated.Data.DataMap.newBuilder().mergeFrom(input);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error converting bytes array to data",e);
        }
        generated.Data.DataMap dataMap = builder.build();
        retVal = pbMapToJavaData(dataMap);
        return retVal;
    }

}
