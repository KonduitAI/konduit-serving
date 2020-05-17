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
import ai.konduit.serving.pipeline.util.ObjectMappers;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import org.nd4j.shade.guava.base.Strings;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.core.http.HttpHeaders.ACCEPT;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
public class InferenceHttpApi {

    PipelineExecutor pipelineExecutor;

    private Data extractData(String contentType, RoutingContext ctx) {
        try {
            if (contentType.contains(APPLICATION_JSON.toString())) {
                return Data.fromJson(ctx.getBodyAsString());
            } else if (contentType.contains(APPLICATION_OCTET_STREAM.toString())) {
                return Data.fromBytes(ctx.getBody().getBytes());
            } else {
                KonduitServingHttpException exception = new KonduitServingHttpException(HttpApiErrorCode.INVALID_CONTENT_TYPE_HEADER,
                        String.format("Invalid Content-Type header %s. Should be one of [application/json, application/octet-stream]", contentType));
                sendErrorResponse(ctx, exception.getErrorResponse());
                throw exception;
            }
        } catch (Exception exception) {
            KonduitServingHttpException konduitServingHttpException =
                    new KonduitServingHttpException(HttpApiErrorCode.DATA_PARSING_ERROR, exception.toString());
            sendErrorResponse(ctx, konduitServingHttpException.getErrorResponse());

            throw exception;
        }
    }

    //TODO: add swagger related annotations to this method or update this class for better swagger annotations support
    public void predict(RoutingContext ctx) {
        String contentType = ctx.request().headers().get(CONTENT_TYPE);
        String accept = ctx.request().headers().get(ACCEPT);

        if(Strings.isNullOrEmpty(contentType)) {
            KonduitServingHttpException exception = new KonduitServingHttpException(HttpApiErrorCode.MISSING_CONTENT_TYPE_HEADER,
                    "Content-Type header should not be null. Possible values are: [application/json, application/octet-stream]");
            sendErrorResponse(ctx, exception.getErrorResponse());

            throw exception;
        }

        if(Strings.isNullOrEmpty(accept)) {
            KonduitServingHttpException exception = new KonduitServingHttpException(HttpApiErrorCode.MISSING_ACCEPT_HEADER,
                    "Accept header should not be null. Possible values are: [application/json, application/octet-stream]");
            sendErrorResponse(ctx, exception.getErrorResponse());

            throw exception;
        }

        Data input = extractData(contentType, ctx);
        Data output;

        try {
            output = pipelineExecutor.exec(input);
        } catch (Exception exception) {
            KonduitServingHttpException konduitServingHttpException =
                    new KonduitServingHttpException(HttpApiErrorCode.PIPELINE_PROCESSING_ERROR, exception.toString());
            sendErrorResponse(ctx, konduitServingHttpException.getErrorResponse());

            throw exception;
        }

        if(accept.contains(APPLICATION_JSON.toString())) {
            ctx.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, accept)
                    .end(output.toJson(), StandardCharsets.UTF_8.name());
        } else if(accept.contains(APPLICATION_OCTET_STREAM.toString())) {
            ctx.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, accept)
                    .end(Buffer.buffer(output.toProtoData().asBytes()));
        } else {
            KonduitServingHttpException exception = new KonduitServingHttpException(HttpApiErrorCode.INVALID_ACCEPT_HEADER,
                    String.format("Invalid Accept header %s. Should be one of [application/json, application/octet-stream]", accept));
            sendErrorResponse(ctx, exception.getErrorResponse());

            throw exception;
        }
    }

    private void sendErrorResponse(RoutingContext ctx, ErrorResponse errorResponse) {
        sendErrorResponse(ctx, errorResponse.getErrorCode(), errorResponse.getErrorMessage());
    }

    private void sendErrorResponse(RoutingContext ctx, HttpApiErrorCode errorCode, String errorMessage) {
        ctx.response()
                .setStatusCode(500)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
                .end(ObjectMappers.toJson(ErrorResponse.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build()));
    }
}
