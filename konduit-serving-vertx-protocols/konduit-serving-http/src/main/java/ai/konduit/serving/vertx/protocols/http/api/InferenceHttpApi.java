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

import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.core.http.HttpHeaders.ACCEPT;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
public class InferenceHttpApi {

    Context context;
    PipelineExecutor pipelineExecutor;

    private Data extractData(String contentType, RoutingContext ctx) {

        if(APPLICATION_JSON.toString().equals(contentType)) {
            return Data.fromJson(ctx.getBodyAsString());
        } else {
            return Data.fromBytes(ctx.getBody().getBytes());
        }
    }

    //TODO: add swagger related annotations to this method or update this class for better swagger annotations support
    public void predict(RoutingContext ctx) {
        String contentType = ctx.request().headers().get(CONTENT_TYPE);
        String accepts = ctx.request().headers().get(ACCEPT);

        Data output = pipelineExecutor.exec(context, extractData(contentType, ctx));

        if(APPLICATION_OCTET_STREAM.toString().equals(accepts)) {
            ctx.response().setStatusCode(200).end(output.toJson(), StandardCharsets.UTF_8.name());
        } else {
            ctx.response().setStatusCode(200).end(Buffer.buffer(output.toProtoData().asBytes()));
        }
    }
}
