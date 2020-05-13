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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@AllArgsConstructor
public abstract class BaseImageFile {

    @Getter
    protected ByteBuffer fileBytes;
    protected Integer height;
    protected Integer width;

    public BaseImageFile(File file) {
        this(file, null, null);
    }

    public BaseImageFile(File file, Integer height, Integer width){
        try {
            fileBytes = ByteBuffer.wrap(FileUtils.readFileToByteArray(file));
        } catch (IOException e){
            throw new DataLoadingException("Unable to load " + formatName() + " image from file " + file.getAbsolutePath());
        }
        this.height = height;
        this.width = width;
    }

    public BaseImageFile(byte[] bytes){
        this(bytes, null, null);
    }

    public BaseImageFile(byte[] bytes, Integer height, Integer width){
        this.fileBytes = ByteBuffer.wrap(bytes);
        this.height = height;
        this.width = width;
    }

    public abstract String formatName();

    public int height(){
        initHW();
        return height;
    }

    public int width(){
        initHW();
        return width;
    }

    protected void initHW(){
        if(height != null && width != null)
            return;
        BufferedImage bi = Image.create(this).getAs(BufferedImage.class);
        height = bi.getHeight();
        width = bi.getWidth();
    }


    public byte[] getBytes(){
        if(fileBytes.hasArray()){
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
