/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

/**
 * Util class for processing file uploads
 *
 * @author Adam Gibson
 */
public class UploadUtils {

    /**
     * Create an upload buffer based
     * on the given parts, the file names,
     * the part name, and the boundary
     * @param fileData the parts of the upload
     * @param fileNames the names of the files
     * @param names the names of the parts
     * @param boundary the boundary for the request
     * @return the whole upload
     */
    public static Buffer uploadBufferFor(Buffer[] fileData,
                                         String[] fileNames,
                                         String[] names,String boundary) {
        Buffer buffer = Buffer.buffer();
        String contentType = "application/octet-stream";

        if (fileData.length != fileNames.length && fileData.length != names.length) {
            throw new IllegalArgumentException("File names data and names must be same length!");
        }

        for (int i = 0; i < fileNames.length; i++) {
            Buffer part = fileData[i];
            String header =
                    "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"" + names[i] + "\"; filename=\"" + fileNames[i] + "\"\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Transfer-Encoding: binary\r\n" +
                            "\r\n";
            buffer.appendString(header);
            buffer.appendBuffer(part);
            if(i < fileNames.length - 1)
                buffer.appendString("\r\n");

        }


        String footer = "\r\n--" + boundary + "--";
        buffer.appendString(footer);
        return buffer;
    }


    /**
     * Set the content length and type for the request
     * @param req the request to set
     * @param buffer the buffer to use to set the request
     * @param boundary the boundary fofr the upload
     */
    public static void prepareRequestFor(HttpClientRequest req, Buffer buffer,String boundary) {
        req.headers().set("Content-Length", String.valueOf(buffer.length()));
        req.headers().set("Content-Type", "multipart/form-data; boundary=" + boundary);
        req.end(buffer);
    }

}
