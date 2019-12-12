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

package ai.konduit.serving.input.converter.tensorflow;

import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.imports.graphmapper.tf.TFGraphMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A tensorflow proto file to
 * {@link SameDiff} handler.
 * This will convert an uploaded
 * file to a {@link SameDiff}
 * flat buffers file exported from samediff's as flatbuffers method
 *
 * @author Adam Gibson
 */
public class TensorflowSameDiffHandler implements Handler<RoutingContext> {


    @Override
    public void handle(RoutingContext event) {
        File kerasFile = getTmpFileWithContext(event);

        try {
            SameDiff sameDiff = TFGraphMapper.importGraph(kerasFile);
            ByteBuffer byteBuffer = sameDiff.asFlatBuffers(true);
            Buffer buffer = Buffer.buffer(Unpooled.wrappedBuffer(byteBuffer));
            File newFile = new File("tmpFile-" + UUID.randomUUID().toString() + ".xml");
            FileUtils.writeByteArrayToFile(newFile, buffer.getBytes());
            event.response().sendFile(newFile.getAbsolutePath(), resultHandler -> {
                if (resultHandler.failed()) {
                    resultHandler.cause().printStackTrace();
                    event.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                } else {
                    event.response().setStatusCode(200);
                }
            });


            event.response().exceptionHandler(exception -> {
                exception.printStackTrace();
            });

        } catch (Exception e) {
            event.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            event.response().setStatusMessage("Error importing model " + e.getMessage());
        }
    }


    protected File getTmpFileWithContext(RoutingContext req) {
        File tmpFile = new File(UUID.randomUUID().toString());
        Buffer buff = req.getBody();
        tmpFile.deleteOnExit();
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buff.getBytes());
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(byteArrayInputStream, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            tmpFile.delete();
            req.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            req.response().setStatusMessage(e.getMessage());
            return null;
        }

        return tmpFile;
    }
}
