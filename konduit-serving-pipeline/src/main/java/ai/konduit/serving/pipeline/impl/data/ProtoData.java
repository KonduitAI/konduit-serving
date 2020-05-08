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
import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import ai.konduit.serving.pipeline.impl.data.helpers.ProtobufUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage;
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
        else {
            throw new UnsupportedOperationException("ProtoData(Data data) constructor not supported");
        }
    }

    @Override
    public ProtoData toProtoData() {
        return this;
    }

    @Override
    public void save(File toFile) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(toFile))) {
            write(os);
        }
    }

    @Override
    public void write(OutputStream toStream) throws IOException {

        DataProtoMessage.DataMap newItemsMap =
                ProtobufUtils.serialize(getDataMap());

        DataProtoMessage.DataMap pbDataMap = DataProtoMessage.DataMap.newBuilder().
                putAllMapItems(newItemsMap.getMapItemsMap()).
                build();
        pbDataMap.writeTo(toStream);

        /*if (hasMetaData()) {
            getMetaData().write(toStream);
        }*/
    }

    public static Data fromFile(File fromFile) throws IOException {
        try (InputStream is = new FileInputStream(fromFile)) {
            return fromStream(is);
        }
    }

    public static Data fromStream(InputStream stream) throws IOException {
        // mergeFrom performs stream buffering internally
        DataProtoMessage.DataMap.Builder builder = DataProtoMessage.DataMap.newBuilder().mergeFrom(stream);
        DataProtoMessage.DataMap dataMap = builder.build();
        return ProtobufUtils.deserialize(dataMap);
    }


    public byte[] asBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(baos);
        } catch (IOException e) {
            String errorText = "Failed write to ByteArrayOutputStream";
            log.error(errorText, e);
            throw new DataLoadingException(errorText);
        }
        return baos.toByteArray();
    }

    public static Data fromBytes(byte[] input) {
        Data retVal = empty();
        DataProtoMessage.DataMap.Builder builder = null;
        try {
            builder = DataProtoMessage.DataMap.newBuilder().mergeFrom(input);
        } catch (InvalidProtocolBufferException e) {
            String errorText = "Error converting bytes array to data";
            log.error(errorText,e);
            throw new DataLoadingException(errorText);
        }
        DataProtoMessage.DataMap dataMap = builder.build();
        retVal = ProtobufUtils.deserialize(dataMap);
        return retVal;
    }

}
