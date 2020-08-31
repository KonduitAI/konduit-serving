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

package ai.konduit.serving.pipeline.impl.data.image.base;

import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
@Slf4j
public abstract class BaseImageFile {

    @Getter
    protected ByteBuffer fileBytes;
    protected Integer height;
    protected Integer width;
    protected Integer channels;

    public BaseImageFile(File file) {
        this(file, null, null,null);
    }

    public BaseImageFile(File file, Integer height, Integer width,Integer channels) {
        try {
            fileBytes = ByteBuffer.wrap(FileUtils.readFileToByteArray(file));
        } catch (IOException e){
            throw new DataLoadingException("Unable to load " + formatName() + " image from file " + file.getAbsolutePath());
        }
        this.height = height;
        this.width = width;
        this.channels = channels;
    }

    public BaseImageFile(byte[] bytes){
        this(bytes, null, null,null);
    }

    public BaseImageFile(byte[] bytes, Integer height, Integer width,Integer channels){
        this(ByteBuffer.wrap(bytes), height, width,channels);
    }

    public BaseImageFile(ByteBuffer fileBytes){
        this(fileBytes, null, null,null);
    }

    public BaseImageFile(ByteBuffer fileBytes, Integer height, Integer width,Integer channels) {
        this.fileBytes = fileBytes;
        this.height = height;
        this.width = width;
        this.channels = channels;
    }

    public abstract String formatName();

    public int channels() {
        initHW();
        return channels;
    }

    public int height() {
        initHW();
        return height;
    }

    public int width() {
        initHW();
        return width;
    }

    protected void initHW() {
        if(height != null && width != null)
            return;
        BufferedImage bi = Image.create(this).getAs(BufferedImage.class);
        height = bi.getHeight();
        width = bi.getWidth();
        switch(bi.getType()) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                channels = 3;
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                log.warn("Note: Loaded image resolved to a channel with an alpha channel. Defaulting to 3 channels. Konduit Serving currently ignores the alpha channel (which normally would be a 4th channel.)");
                channels = 4 - 1;
                break;
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_INDEXED:
            case BufferedImage.TYPE_USHORT_GRAY:
                channels = 1;
                break;
            case BufferedImage.TYPE_CUSTOM:
               channels = 3;
               log.warn("Note: Loaded image resolved to type custom with BufferedImage. Defaulting to 3 channels for custom image type.");
               break;

        }
    }


    public byte[] getBytes() {
        if(fileBytes.hasArray()) {
            return fileBytes.array();
        } else {
            byte[] bytes = new byte[fileBytes.capacity()];
            fileBytes.position(0);
            fileBytes.get(bytes);
            return bytes;
        }

    }

    public void save(File f) throws IOException {
        FileUtils.writeByteArrayToFile(f, getBytes());
    }

    public void write(OutputStream os) throws IOException {
        boolean buffered = os instanceof BufferedOutputStream;
        if(!buffered)
            os = new BufferedOutputStream(os);
        try(OutputStream o = os){
            o.write(getBytes());
        }
    }
}
