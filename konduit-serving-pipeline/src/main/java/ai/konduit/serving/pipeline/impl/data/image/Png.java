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

package ai.konduit.serving.pipeline.impl.data.image;

import ai.konduit.serving.pipeline.api.exception.DataLoadingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
public class Png {

    private ByteBuffer pngFileBytes;

    public Png(File file){
        try {
            pngFileBytes = ByteBuffer.wrap(FileUtils.readFileToByteArray(file));
        } catch (IOException e){
            throw new DataLoadingException("Unable to load PNG image from file " + file.getAbsolutePath());
        }
    }

    public Png(byte[] bytes){
        this.pngFileBytes = ByteBuffer.wrap(bytes);
    }

    public byte[] getBytes(){
        if(pngFileBytes.hasArray()){
            return pngFileBytes.array();
        } else {
            byte[] bytes = new byte[pngFileBytes.capacity()];
            pngFileBytes.position(0);
            pngFileBytes.get(bytes);
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
