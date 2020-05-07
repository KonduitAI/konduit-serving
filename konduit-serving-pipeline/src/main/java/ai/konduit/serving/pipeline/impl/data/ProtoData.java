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
import ai.konduit.serving.pipeline.impl.data.helpers.ProtobufUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class ProtoData extends JData {

    public ProtoData(@NonNull InputStream stream) throws IOException {
        this(fromStream(stream));
    }

    public ProtoData(@NonNull byte[] input) {
        this(fromBytes(input));
    }

    public ProtoData(@NonNull File file) throws IOException {
        this(fromFile(file));
    }

    public ProtoData(@NonNull Data data) {
        if(data instanceof JData){
            getDataMap().putAll(((JData)data).getDataMap());
        }
    }

    @Override
    public ProtoData toProtoData() {
        return this;
    }

    @Override
    public void save(File toFile) throws IOException {
        write(new FileOutputStream(toFile));
    }

    @Override
    public void write(OutputStream toStream) throws IOException {

        generated.Data.DataMap newItemsMap =
                ProtobufUtils.convertJavaToProtobufData(getDataMap());

        generated.Data.DataMap pbDataMap = generated.Data.DataMap.newBuilder().
                putAllMapItems(newItemsMap.getMapItemsMap()).
                build();
        pbDataMap.writeTo(toStream);
    }

    public static Data fromFile(File fromFile) throws IOException {
        generated.Data.DataMap.Builder builder = generated.Data.DataMap.newBuilder().mergeFrom(new FileInputStream(fromFile));
        generated.Data.DataMap dataMap = builder.build();
        return ProtobufUtils.convertProtobufToData(dataMap);
    }

    public static Data fromStream(InputStream stream) throws IOException {
        generated.Data.DataMap.Builder builder = generated.Data.DataMap.newBuilder().mergeFrom(stream);
        generated.Data.DataMap dataMap = builder.build();
        return ProtobufUtils.convertProtobufToData(dataMap);
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
        retVal = ProtobufUtils.convertProtobufToData(dataMap);
        return retVal;
    }

}
