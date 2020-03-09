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

package ai.konduit.serving.input.converter;

import ai.konduit.serving.input.converter.keras.KerasDl4jHandler;
import ai.konduit.serving.input.converter.pmml.RPmmlHandler;
import ai.konduit.serving.input.converter.pmml.SkLearnPmmlHandler;
import ai.konduit.serving.input.converter.pmml.XgboostPmmlHandler;
import ai.konduit.serving.input.converter.tensorflow.TensorflowSameDiffHandler;
import ai.konduit.serving.verticles.base.BaseRoutableVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;


/**
 * A {@link io.vertx.core.Verticle} for defining a set of converters for
 * various kinds of files to different file formats.
 * <p>
 * This {@link io.vertx.core.Verticle} expects 2 arguments in its json configuration:
 * <p>
 * uploadKey: This is the field in the configuration that should contain
 * the upload directory. The default value for this is just
 * the current working directory/upload.
 * <p>
 * unzipKey: The place to unzip files to.
 * This is for handling endpoints that require uploading a zip file
 * to extract the contents.
 *
 * @author Adam Gibson
 */
@Slf4j
public class ConverterVerticle extends BaseRoutableVerticle {


    public final static String UPLOAD_KEY = "uploadKey";
    public final static String DEFAULT_UPLOAD_PATH = "upload";
    public final static String DEFAULT_UNZIP_DIR = "unzip";
    public final static String UNZIP_DIR = "unzipKey";
    protected String filePath;
    protected String unzipPath;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

        router = Router.router(vertx);
        filePath = config().getString(UPLOAD_KEY) == null ? DEFAULT_UPLOAD_PATH : config().getString(UPLOAD_KEY);
        unzipPath = config().getString(UNZIP_DIR) == null ? DEFAULT_UNZIP_DIR : config().getString(UNZIP_DIR);

        router.route().handler(BodyHandler.create()
                .setMergeFormAttributes(true)
                .setDeleteUploadedFilesOnEnd(true)
                .setUploadsDirectory(filePath));


        router.post("/pmml/convert/sklearn")
                .handler(new SkLearnPmmlHandler())
                .failureHandler(failure -> {
                    log.error("Failed to convert ", failure.failure());
                });

        router.post("/pmml/convert/xgboost")
                .handler(new XgboostPmmlHandler(new File(unzipPath)))
                .failureHandler(failure -> {
                    log.error("Failed to convert ", failure.failure());
                });

        router.post("/pmml/convert/r/:" + RPmmlHandler.CONVERTER_TYPE)
                .handler(new RPmmlHandler())
                .failureHandler(failure -> {
                    log.error("Failed to convert ", failure.failure());
                });

        router.post("/keras/:" + KerasDl4jHandler.MODEL_TYPE).handler(new KerasDl4jHandler())
                .failureHandler(failure -> {
                    log.error("Failed to convert ", failure.failure());
                });

        router.post("/tensorflow/").handler(new TensorflowSameDiffHandler())
                .failureHandler(failure -> {
                    log.error("Failed to convert ", failure.failure());
                });
    }
}
