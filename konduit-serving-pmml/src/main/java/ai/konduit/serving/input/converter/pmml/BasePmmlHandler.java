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

package ai.konduit.serving.input.converter.pmml;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Base class for handling pmml input.
 * This class takes in a raw buffer
 * and handles using various converter utilities
 * to convert objects to {@link org.dmg.pmml.PMML}
 * based on the parser from jpmml.
 *
 * There are various sub classes that then parse the
 * expected input differently to output pmml.
 *
 *
 * @author Adam Gibson
 */
@Slf4j
public abstract class BasePmmlHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext req) {

        Object[] extraArgs = getExtraArgs(req);
        if(extraArgs == null) {
            return;
        }

        try {

            Buffer writeBuffer = getPmmlBuffer(req,getExtraArgs(req));
            File newFile = new File(System.getProperty("java.io.tmpdir"),"tmpFile-" + UUID.randomUUID().toString() + ".xml");
            newFile.deleteOnExit();
            FileUtils.writeByteArrayToFile(newFile,writeBuffer.getBytes());
            req.response().sendFile(newFile.getAbsolutePath(),resultHandler -> {
                if(resultHandler.failed()) {
                    if(resultHandler.cause() != null)
                        resultHandler.cause().printStackTrace();
                    else {
                        log.warn("No error found. Failed to convert pmml.");
                    }
                    req.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                }
                else {
                    req.response().setStatusCode(200);
                }
            });


            req.response().exceptionHandler(exception -> {
                exception.printStackTrace();
            });
        } catch (Exception e) {
            req.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            req.response().setStatusMessage("Failed to convert " + e.getMessage());
            return;
        }
    }


    public abstract Buffer getPmmlBuffer(RoutingContext routingContext,Object...otherInputs) throws Exception;


    public abstract Object[] getExtraArgs(RoutingContext req);

    protected File getTmpFileWithContext(RoutingContext req) {
        File tmpFile = new File(UUID.randomUUID().toString());
        Buffer buff = req.getBody();
        tmpFile.deleteOnExit();
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buff.getBytes());
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(byteArrayInputStream,fileOutputStream);
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
