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

package ai.konduit.serving.verticles.nd4j.memmap;

import ai.konduit.serving.verticles.VerticleConstants;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.netty.buffer.Unpooled;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.LocationPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.serde.binary.BinarySerde;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A verticle for memory mapped arrays.
 * Useful for returning json or binary (numpy or nd4j via {@link BinarySerde}
 * views of a big memory mapped ndarray.
 *
 *
 * @author Adam Gibson
 */
@Slf4j
public class MemMapVerticle extends BaseRoutableVerticle {

    public final static String ARRAY_URL = "arrayPath";
    public final static String INITIAL_MEM_MAP_SIZE = "initialMemmapSize";
    public final static long DEFAULT_INITIAL_SIZE = 1000000000;
    public final static String WORKSPACE_NAME = "memMapWorkspace";
    private INDArray unkVector;
    private ThreadLocal<INDArray> arr;


    private WorkspaceConfiguration mmap;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        String configValue = config().getValue(INITIAL_MEM_MAP_SIZE,String.valueOf(DEFAULT_INITIAL_SIZE)).toString();
        Long initialSize = Long.parseLong(configValue);
        if(!config().containsKey(ARRAY_URL)) {
            throw new IllegalStateException("No array found! Please specify an arrayPath");
        }


        if(context.config().containsKey(VerticleConstants.MEM_MAP_VECTOR_PATH)) {
            String path =  context.config().getString(VerticleConstants.MEM_MAP_VECTOR_PATH);
            try {
                byte[] content = FileUtils.readFileToByteArray(new File(path));
                unkVector = Nd4j.createNpyFromByteArray(content);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load unknown vector: " + path);
            }
        }


        arr = new ThreadLocal<>();
        mmap = WorkspaceConfiguration.builder()
                .initialSize(initialSize)
                .policyLocation(LocationPolicy.MMAP)
                .build();


        router.post("/array/:arrayType")
                .handler(ctx -> {
                    INDArray write  = getOrSetArrForContext();
                    String paramType = ctx.pathParam("arrayType");
                    if(paramType.equals("json"))
                        writeArrayJson(write,ctx);
                    else
                        writeArrayBinary(write,ctx);
                });



        router.post("/array/indices/:arrayType")
                .handler(ctx -> {
                    INDArray write  = getArrayFromContext(ctx);
                    String paramType = ctx.pathParam("arrayType");
                    if(paramType.equals("json"))
                        writeArrayJson(write,ctx);
                    else
                        writeArrayBinary(write,ctx);
                });

        router.post("/array/range/:from/:to/:arrayType")
                .handler(ctx -> {
                    INDArray write  = getArrayFromContextRange(ctx);
                    String paramType = ctx.pathParam("arrayType");
                    if(paramType.equals("json"))
                        writeArrayJson(write,ctx);
                    else
                        writeArrayBinary(write,ctx);
                });

        setupWebServer();

    }



    private void writeArrayJson(INDArray write,RoutingContext ctx) {
        ctx.response().putHeader("Content-Type","application/json");
        ctx.response().setChunked(false);
        ctx.response().end(write.toString());
    }

    private void writeArrayBinary(INDArray write, RoutingContext ctx) {
        if(write.length() == 1) {
            write = write.reshape(new int[]{1,1});
        }

        Buffer writeBuffer = null;
        String arrayType = ctx.pathParam("arrayType");

        switch (arrayType) {
            case "numpy":
                try {
                    Pointer convert = Nd4j.getNDArrayFactory().convertToNumpy(write);
                    BytePointer cast = new BytePointer(convert);
                    writeBuffer = Buffer.buffer(Unpooled.wrappedBuffer(cast.getStringBytes()));
                } catch (Exception e) {
                    log.error("Error creating numpy array",e);
                    ctx.response().setStatusCode(500);
                }
                break;
            case "nd4j":
                try {
                    writeBuffer = Buffer.buffer(Unpooled.wrappedBuffer(BinarySerde.toByteBuffer(write)));
                } catch (Exception e) {
                    log.error("Error creating nd4j array",e);
                    ctx.response().setStatusCode(500);

                }

                break;
        }

        ctx.response().setChunked(false);
        ctx.response().end(writeBuffer);

    }



    private INDArray getArrayFromContextRange(RoutingContext ctx) {
        ctx.response().setStatusCode(200);
        ctx.response().setChunked(false);
        int from = Integer.parseInt(ctx.pathParam("from"));
        int to = Integer.parseInt(ctx.pathParam("to"));

        INDArray arr = getOrSetArrForContext();
        if(arr.isVector()) {
            INDArrayIndex[] indices = new INDArrayIndex[1];
            indices[0] = NDArrayIndex.interval(from,to);
            INDArray write  = arr.get(indices);
            return write;
        }
        else {
            INDArrayIndex[] indices = new INDArrayIndex[arr.rank()];
            for(int i = 0; i < indices.length; i++) {
                indices[i] = NDArrayIndex.all();
            }

            indices[0] = NDArrayIndex.interval(from,to);
            INDArray write  = arr.get(indices);
            return write;
        }


    }

    private INDArray getOrSetArrForContext() {
        String path = config().getString(ARRAY_URL);


        File loadFrom = new File(path);
        if(!loadFrom.exists()) {
            throw new IllegalStateException("File not found at path " + path);
        }

        if(arr.get() == null) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(mmap, WORKSPACE_NAME)) {
                if(path.endsWith("npy"))
                    arr.set((Nd4j.createFromNpyFile(loadFrom)));
                else {
                    arr.set(BinarySerde.readFromDisk(loadFrom));
                }
            } catch (IOException e) {
                log.error("Error creating nd4j array", e);
            }
        }

        return arr.get();
    }

    private INDArray getArrayFromContext(RoutingContext ctx) {
        ctx.response().setStatusCode(200);
        ctx.response().setChunked(false);
        JsonArray bodyAsJson = ctx.getBodyAsJsonArray();

        if(bodyAsJson == null) {
            throw new IllegalStateException("No body found!");
        }

        INDArray arr = getOrSetArrForContext();


        JsonArray jsonArray = bodyAsJson;
        List<INDArray> slices =  new ArrayList<>();
        for(int i = 0; i < jsonArray.size(); i++) {
            int idx = jsonArray.getInteger(i);
            if(idx < 0) {
                if(unkVector != null) {
                    slices.add(unkVector);
                }
                else {
                    ctx.response().setStatusCode(400);
                    ctx.response().setStatusMessage("Unknown vector specified, but server did not have one configured. Please specify a vector upon startup.");
                    ctx.response().setChunked(false);
                }
            }
            else
                slices.add(arr.slice(idx));
        }

        INDArray write  = Nd4j.concat(0,slices.toArray(new INDArray[slices.size()]));
        return write;
    }

}
