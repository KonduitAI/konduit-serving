/*
 *
 *  * ******************************************************************************
 *  *
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

package ai.konduit.serving.configprovider;


import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.MemMapConfig;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.base.Preconditions;
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
 * MemMap Route Definer handles implementing
 * endpoints for returning views of a large
 * memory mapped {@link INDArray}
 * loaded via nd4j's {@link LocationPolicy#MMAP}
 * this means that nd4j will use a memory mapped
 * workspace for loading an array from disk.
 * <p>
 * This route definer keeps a {@link ThreadLocal}
 * of {@link INDArray} in memory using nd4j's
 * workspace described above.
 * When this class is initialized, an array will
 * be loaded from disk based on the passed in {@link MemMapConfig}
 *
 * @author Adam Gibson
 */
@Slf4j
public class MemMapRouteDefiner {

    private INDArray unknownVector;
    private ThreadLocal<INDArray> arr;
    private MemMapConfig memMapConfig;

    private WorkspaceConfiguration mmap;

    /**
     * Define the routes implementing
     * memory mapped operations.
     * This will also load the array from disk
     * and use the {@link MemMapConfig}
     * specified to initialize the array.
     * <p>
     * The routes defined are as follows:
     * /array/:arrayType -&gt; where arrayType is a parameter value of json or binary.
     * If json is specified {@link #writeArrayJson(INDArray, RoutingContext)}
     * is called otherwise {@link #writeArrayBinary(INDArray, RoutingContext)} is called.
     * <p>
     * This will return the whole array in memory. Note this is likely to be big.
     * <p>
     * /array/indices/:arrayType : where arrayType is a parameter value of json or binary.
     * If json is specified {@link #writeArrayJson(INDArray, RoutingContext)}
     * is called otherwise {@link #writeArrayBinary(INDArray, RoutingContext)} is called.
     * Indices expects a post body of a json array containing a list of indices to return.
     * The indices will be used to determine what slices to return from an {@link INDArray}
     * <br>
     * <p>
     * /array/range/:from/:to/:arrayType where arrayType is a parameter value of json or binary.
     * If json is specified {@link #writeArrayJson(INDArray, RoutingContext)}
     * is called otherwise {@link #writeArrayBinary(INDArray, RoutingContext)} is called.
     * from and to are integers representing a range. Similarly to indices
     * this will return the slices from the range to the given range.
     *
     * @param vertx                  the vertx instance to use to define the routes
     * @param inferenceConfiguration the {@link InferenceConfiguration}
     *                               to use for configuration.
     * @return the router with the endpoints defined
     */
    public Router defineRoutes(Vertx vertx, InferenceConfiguration inferenceConfiguration) {
        Long initialSize = inferenceConfiguration.getMemMapConfig().getInitialMemmapSize();
        Router router = Router.router(vertx);
        memMapConfig = inferenceConfiguration.getMemMapConfig();

        String path =  inferenceConfiguration.getMemMapConfig().getUnkVectorPath();
       if(path != null) {
           try {
               byte[] content = FileUtils.readFileToByteArray(new File(path));
               unknownVector = Nd4j.createNpyFromByteArray(content);
           } catch (IOException e) {
               throw new IllegalStateException("Unable to load unknown vector: " + path);
           }
       }

        File tempFile = new File(System.getProperty("user.home"), ".mmap-temp-file");
        if (!tempFile.exists()) {
            try {
                Preconditions.checkState(tempFile.createNewFile(), String.format("Memmap temp file at path %s wasn't able to be created successfully. " +
                        "Check that if you have write permissions to that file location", tempFile.getAbsolutePath()));
            } catch (IOException e) {
                log.error(String.format("Unable to create file at location: %s", tempFile.getAbsolutePath()), e);
            }
        }

        Preconditions.checkState(tempFile.canWrite() && tempFile.canRead(), String.format("Unable to either read or write to %s for memmap temp file.",
                tempFile.getAbsolutePath()));

        arr = new ThreadLocal<>();
        mmap = WorkspaceConfiguration.builder()
                .initialSize(initialSize)
                .policyLocation(LocationPolicy.MMAP)
                .tempFilePath(tempFile.getAbsolutePath())
                .build();

        router.post().handler(BodyHandler.create()
                .setUploadsDirectory(inferenceConfiguration.getServingConfig().getUploadsDirectory())
                .setDeleteUploadedFilesOnEnd(true)
                .setMergeFormAttributes(true))
                .failureHandler(failureHandlder -> {
                    if (failureHandlder.statusCode() == 404) {
                        log.warn("404 at route " + failureHandlder.request().path());
                    } else if (failureHandlder.failed()) {
                        if (failureHandlder.failure() != null) {
                            log.error("Request failed with cause ", failureHandlder.failure());
                        } else {
                            log.error("Request failed with unknown cause.");
                        }
                    }
                });


        router.get("/healthcheck*").handler(HealthCheckHandler.create(vertx));

        router.get("/config")
                .produces("application/json").handler(ctx -> {
            try {
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().end(vertx.getOrCreateContext().config().encode());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        router.get("/config/pretty")
                .produces("application/json").handler(ctx -> {
            try {
                ctx.response().putHeader("Content-Type", "application/json");
                ctx.response().end(vertx.getOrCreateContext().config().encodePrettily());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });


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
                    INDArray write = getArrayFromContextRange(ctx);
                    String paramType = ctx.pathParam("arrayType");
                    if (paramType.equals("json"))
                        writeArrayJson(write, ctx);
                    else
                        writeArrayBinary(write, ctx);
                });

        return router;
    }


    private void writeArrayJson(INDArray write, io.vertx.ext.web.RoutingContext ctx) {
        ctx.response().putHeader("Content-Type", "application/json");
        ctx.response().setChunked(false);
        ctx.response().end(write.toString());
    }

    private void writeArrayBinary(INDArray write, RoutingContext ctx) {
        if (write.length() == 1) {
            write = write.reshape(new int[]{1, 1});
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
                    log.error("Error creating numpy array", e);
                    ctx.response().setStatusCode(500);
                }
                break;
            case "nd4j":
                try {
                    writeBuffer = Buffer.buffer(Unpooled.wrappedBuffer(BinarySerde.toByteBuffer(write)));
                } catch (Exception e) {
                    log.error("Error creating nd4j array", e);
                    ctx.response().setStatusCode(500);

                }

                break;
        }

        ctx.response().putHeader("Content-Type", "application/octet-stream");
        ctx.response().setChunked(false);
        ctx.response().end(writeBuffer);

    }


    private INDArray getArrayFromContextRange(RoutingContext ctx) {
        ctx.response().setStatusCode(200);
        ctx.response().setChunked(false);
        int from = Integer.parseInt(ctx.pathParam("from"));
        int to = Integer.parseInt(ctx.pathParam("to"));

        INDArray arr = getOrSetArrForContext();
        if (arr.isVector()) {
            INDArrayIndex[] indices = new INDArrayIndex[1];
            indices[0] = NDArrayIndex.interval(from, to);
            INDArray write = arr.get(indices);
            return write;
        } else {
            INDArrayIndex[] indices = new INDArrayIndex[arr.rank()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = NDArrayIndex.all();
            }

            indices[0] = NDArrayIndex.interval(from, to);
            INDArray write = arr.get(indices);
            return write;
        }


    }

    private INDArray getOrSetArrForContext() {
        String path = memMapConfig.getArrayPath();


        File loadFrom = new File(path);
        if (!loadFrom.exists()) {
            throw new IllegalStateException("File not found at path " + path);
        }

        if (arr.get() == null) {
            try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(mmap, memMapConfig.getWorkSpaceName())) {
                if (path.endsWith("npy"))
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

        if (bodyAsJson == null) {
            throw new IllegalStateException("No body found!");
        }

        INDArray arr = getOrSetArrForContext();


        JsonArray jsonArray = bodyAsJson;
        List<INDArray> slices = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            int idx = jsonArray.getInteger(i);
            if(idx < 0) {
                if(unknownVector != null) {
                    slices.add(unknownVector);
                }
                else {
                    ctx.response().setStatusCode(400);
                    ctx.response().setStatusMessage("Unknown vector specified, but server did not have one " +
                            "configured. Please specify a vector upon startup.");
                    ctx.response().setChunked(false);
                }
            } else
                slices.add(arr.slice(idx));
        }

        INDArray write = Nd4j.concat(0, slices.toArray(new INDArray[slices.size()]));
        return write;
    }

}
