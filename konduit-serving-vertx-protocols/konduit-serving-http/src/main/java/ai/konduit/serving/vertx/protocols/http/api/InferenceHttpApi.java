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

package ai.konduit.serving.vertx.protocols.http.api;

import ai.konduit.serving.data.nd4j.format.ND4JConverters;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.Image;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.format.JavaImageFactory;
import ai.konduit.serving.pipeline.registry.ImageFactoryRegistry;
import ai.konduit.serving.pipeline.registry.NDArrayConverterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nd4j.shade.guava.base.Strings;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.vertx.core.http.HttpHeaders.ACCEPT;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
@Getter
public class InferenceHttpApi {

    private static double requestTime = -1.0;
    private static double pipelineTime = -1.0;

    protected static Gauge requestTimeGuage = null;
    protected static Gauge pipelineTimeGuage = null;
    protected static Gauge requestThroughputGuage = null;
    protected static Counter requestsHandledCounter = null;

    protected final PipelineExecutor pipelineExecutor;
    protected static MeterRegistry registry = null;

    static {
        ImageFactoryRegistry.addFactory(new JavaImageFactory());
        NDArrayConverterRegistry.addConverter(new ND4JConverters.Nd4jToSerializedConverter());
        NDArrayConverterRegistry.addConverter(new ND4JConverters.SerializedToNd4jArrConverter());
    }

    public static Data extractData(String contentType, RoutingContext ctx) {
        try {
            if (contentType.contains(APPLICATION_JSON.toString())) {
                return Data.fromJson(ctx.getBodyAsString(StandardCharsets.UTF_8.name()));
            } else if (contentType.contains(APPLICATION_OCTET_STREAM.toString())) {
                return Data.fromBytes(ctx.getBody().getBytes());
            } else if(contentType.contains(MULTIPART_FORM_DATA.toString())) {
                StringBuilder stringBuilder = new StringBuilder("{");
                ctx.request().formAttributes().forEach(entry -> stringBuilder.append(String.format(",\"%s\":%s", entry.getKey(), entry.getValue().startsWith("[") ? entry.getValue() : String.format("\"%s\"", entry.getValue()))));
                stringBuilder.append("}");
                Data data = Data.fromJson(stringBuilder.toString().replaceFirst(",",""));

                for(FileUpload fileUpload: ctx.fileUploads()) {
                    if(StringUtils.containsIgnoreCase(fileUpload.contentType(), "image")) {
                        data.put(fileUpload.name(), Image.create(ImageIO.read(new File(fileUpload.uploadedFileName()))));
                    } else {
                        data.put(fileUpload.name(), FileUtils.readFileToString(new File(fileUpload.uploadedFileName()), StandardCharsets.UTF_8));
                    }
                }

                return data;
            } else {
                throw new KonduitServingHttpException(HttpApiErrorCode.INVALID_CONTENT_TYPE_HEADER,
                        String.format("Invalid Content-Type header %s. Should be one of [application/json, application/octet-stream, multipart/form-data]", contentType));
            }
        } catch (Exception exception) {
            throw new KonduitServingHttpException(HttpApiErrorCode.DATA_PARSING_ERROR,
                    String.format("%s. More Details: %s",
                            exception.toString(),
                            exception.getCause() != null ? exception.getCause().getMessage() : "null"));
        }
    }

    //TODO: add swagger related annotations to this method or update this class for better swagger annotations support
    public void predict(RoutingContext ctx) {
        double requestTimeStart = (double) System.currentTimeMillis();
        String contentType = ctx.request().headers().get(CONTENT_TYPE);
        String accept = ctx.request().headers().get(ACCEPT);

        if(Strings.isNullOrEmpty(contentType)) {
            throw new KonduitServingHttpException(HttpApiErrorCode.MISSING_OR_EMPTY_CONTENT_TYPE_HEADER,
                    "Content-Type header should not be null. Possible values are: [application/json, application/octet-stream, multipart/form-data]");
        }

        if(Strings.isNullOrEmpty(accept)) {
            throw new KonduitServingHttpException(HttpApiErrorCode.MISSING_OR_EMPTY_ACCEPT_HEADER,
                    "Accept header should not be null. Possible values are: [application/json, application/octet-stream]");
        }

        Data input = extractData(contentType, ctx);
        Data output;

        try {
            double pipelineTimeStart = (double) System.currentTimeMillis();

            output = pipelineExecutor.exec(input);

            double pipelineTimeEnd = (double) System.currentTimeMillis();
            pipelineTime = pipelineTimeEnd - pipelineTimeStart;
        } catch (Exception exception) {
            throw new KonduitServingHttpException(HttpApiErrorCode.PIPELINE_PROCESSING_ERROR, exception);
        }

        if(accept.contains(APPLICATION_JSON.toString())) {
            ctx.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                    .end(output.toJson(), StandardCharsets.UTF_8.name());
        } else if(accept.contains(APPLICATION_OCTET_STREAM.toString())) {
            ctx.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM.toString())
                    .end(Buffer.buffer(output.asBytes()));
        } else {
            throw new KonduitServingHttpException(HttpApiErrorCode.INVALID_ACCEPT_HEADER,
                    String.format("Invalid Accept header %s. Should be one of [application/json, application/octet-stream]", accept));
        }

        if(registry != null) {
            requestsHandledCounter.increment();
        }

        double requestTimeEnd = (double) System.currentTimeMillis();
        requestTime = requestTimeEnd - requestTimeStart;
    }

    public static void setMetrics(MeterRegistry registry, Iterable<Tag> tags) {
        if(registry != null) {
            InferenceHttpApi.registry = registry;
            requestTimeGuage = Gauge.builder("request.time.ms", () -> requestTime).tags(tags).register(registry);
            pipelineTimeGuage = Gauge.builder("pipeline.time.ms", () -> pipelineTime).tags(tags).register(registry);
            requestThroughputGuage = Gauge.builder("request.throughput", () -> 1 / requestTime * 1000).tags(tags).register(registry);

            requestsHandledCounter = registry.counter("requests.handled", tags);
        }
    }
}
