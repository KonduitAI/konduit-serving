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

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.nd4j.shade.guava.base.Strings;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.core.http.HttpHeaders.ACCEPT;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
@Getter
public class InferenceHttpApi {

    protected final PipelineExecutor pipelineExecutor;

    public static Data extractData(String contentType, RoutingContext ctx) {
        try {
            if (contentType.contains(APPLICATION_JSON.toString())) {
                return Data.fromJson(ctx.getBodyAsString());
            } else if (contentType.contains(APPLICATION_OCTET_STREAM.toString())) {
                return Data.fromBytes(ctx.getBody().getBytes());
            } else {
                throw new KonduitServingHttpException(HttpApiErrorCode.INVALID_CONTENT_TYPE_HEADER,
                        String.format("Invalid Content-Type header %s. Should be one of [application/json, application/octet-stream]", contentType));
            }
        } catch (Exception exception) {
            throw new KonduitServingHttpException(HttpApiErrorCode.DATA_PARSING_ERROR, exception.toString());
        }
    }

    //TODO: add swagger related annotations to this method or update this class for better swagger annotations support
    public void predict(RoutingContext ctx) {
        String contentType = ctx.request().headers().get(CONTENT_TYPE);
        String accept = ctx.request().headers().get(ACCEPT);

        if(Strings.isNullOrEmpty(contentType)) {
            throw new KonduitServingHttpException(HttpApiErrorCode.MISSING_OR_EMPTY_CONTENT_TYPE_HEADER,
                    "Content-Type header should not be null. Possible values are: [application/json, application/octet-stream]");
        }

        if(Strings.isNullOrEmpty(accept)) {
            throw new KonduitServingHttpException(HttpApiErrorCode.MISSING_OR_EMPTY_ACCEPT_HEADER,
                    "Accept header should not be null. Possible values are: [application/json, application/octet-stream]");
        }

        Data input = extractData(contentType, ctx);
        Data output;

        try {
            output = pipelineExecutor.exec(input);
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
    }
}
