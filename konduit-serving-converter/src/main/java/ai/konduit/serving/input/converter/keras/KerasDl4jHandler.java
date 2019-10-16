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

package ai.konduit.serving.input.converter.keras;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.*;
import java.util.UUID;

/**
 * A keras to dl4j import handler.
 *
 * The endpoint has 2 path params:
 * sequential and functional
 *
 * This handler will return a direct dl4j zip file
 * based on the the model import output of
 * {@link KerasModelImport}
 *
 * @author Adam Gibson
 */
public class KerasDl4jHandler implements Handler<RoutingContext> {

    public final static String MODEL_TYPE = "modelType";

    public enum ModelType {
        SEQUENTIAL,FUNCTIONAL
    }



    @Override
    public void handle(RoutingContext event) {
        File kerasFile = getTmpFileWithContext(event);
        ModelType type = getTypeFromContext(event);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            switch (type) {
                case FUNCTIONAL:
                    ComputationGraph computationGraph = KerasModelImport.importKerasModelAndWeights(kerasFile.getAbsolutePath());
                    ModelSerializer.writeModel(computationGraph,byteArrayOutputStream,true);
                    break;
                case SEQUENTIAL:
                    MultiLayerNetwork multiLayerConfiguration = KerasModelImport.importKerasSequentialModelAndWeights(kerasFile.getAbsolutePath());
                    ModelSerializer.writeModel(multiLayerConfiguration,byteArrayOutputStream,true);
                    break;
            }

            Buffer buffer = Buffer.buffer(byteArrayOutputStream.toByteArray());
            File newFile = new File("tmpFile-" + UUID.randomUUID().toString() + ".xml");
            FileUtils.writeByteArrayToFile(newFile,buffer.getBytes());
            event.response().sendFile(newFile.getAbsolutePath(),resultHandler -> {
                if(resultHandler.failed()) {
                    resultHandler.cause().printStackTrace();
                    event.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                }
                else {
                    event.response().setStatusCode(200);
                }
            });


            event.response().exceptionHandler(exception -> {
                exception.printStackTrace();
            });

        }
        catch(Exception e) {
            event.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            event.response().setStatusMessage("Error importing model " + e.getMessage());
        }
    }

    protected ModelType getTypeFromContext(RoutingContext routingContext) {
        String modelType = routingContext.pathParam(MODEL_TYPE);
        return ModelType.valueOf(modelType.toUpperCase());
    }


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
