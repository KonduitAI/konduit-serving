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

package ai.konduit.serving.endpoint;

import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * Endpoint represents a single custom HTTP endpoint, as part of a {@link HttpEndpoints} instance, as specified via
 * InferenceConfiguration.<br>
 * <b>>NOTE</b: The API for custom endpoints should be considered experimental and subject to change
 *
 * @author Alex Black
 */
@Schema(description = "An object that represents a single custom HTTP endpoints as specified via " +
        "InferenceConfiguration. Note: The API for custom endpoints should be considered experimental " +
        "and subject to change.")
public interface Endpoint {

    /**
     * @return The endpoint type - for example, GET, POST, etc
     */
    HttpMethod type();

    /**
     * @return The path of the endpoint - for example "/my/custom/endpoint". May include path parameters
     */
    String path();

    /**
     * @return The list of supported input MIME content types (see {@link io.netty.handler.codec.http.HttpHeaderValues}
     */
    List<String> consumes();

    /**
     * @return The list of supported output MIME content types (see {@link io.netty.handler.codec.http.HttpHeaderValues}
     */
    List<String> produces();

    @Schema(description = "The endpoint type - for example, GET, POST, etc.")
    default HttpMethod getType() {
        return type();
    }

    @Schema(description = "The path of the endpoint - for example /my/custom/endpoint. May include path parameters.")
    default String getPath() {
        return path();
    }

    @Schema(description = "The list of supported input MIME content types.")
    default List<String> getConsumes() {
        return consumes();
    }

    @Schema(description = "The list of supported output MIME content types.")
    default List<String> getProduces() {
        return produces();
    }

    /**
     * @return The Vert.x handler for this endpoint
     */
    Handler<RoutingContext> handler();
}
