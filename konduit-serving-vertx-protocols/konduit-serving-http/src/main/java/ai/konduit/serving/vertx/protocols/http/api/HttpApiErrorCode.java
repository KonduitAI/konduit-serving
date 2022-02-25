/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An enum specifying the type of error occured on the konduit serving http server. " +
        "DATA_PARSING_ERROR -> If the incoming data cannot be parsed " +
        "MISSING_OR_EMPTY_CONTENT_TYPE_HEADER -> If the requests has no Content-Type header " +
        "INVALID_CONTENT_TYPE_HEADER -> If the Content-Type header has an invalid value. Currently it should be either application/json or application/octet-stream " +
        "MISSING_OR_EMPTY_ACCEPT_HEADER -> If the request has no Accept header " +
        "INVALID_ACCEPT_HEADER -> If the Accept header has an invalid value. Currently it should be either application/json or application/octet-stream " +
        "PIPELINE_PROCESSING_ERROR -> If there's an error while processing the data through the pipeline.")
public enum HttpApiErrorCode {
    /**
     * If the incoming data cannot be parsed
     */
    DATA_PARSING_ERROR,

    /**
     * If the requests has no Content-Type header
     */
    MISSING_OR_EMPTY_CONTENT_TYPE_HEADER,

    /**
     * If the "Content-Type" header has an invalid value. Currently it should be either application/json or application/octet-stream
     */
    INVALID_CONTENT_TYPE_HEADER,

    /**
     * If the request has no "Accept" header
     */
    MISSING_OR_EMPTY_ACCEPT_HEADER,

    /**
     * If the "Accept" header has an invalid value. Currently it should be either application/json or application/octet-stream
     */
    INVALID_ACCEPT_HEADER,

    /**
     * If there's an error while processing the data through the {@link Pipeline}.
     */
    PIPELINE_PROCESSING_ERROR
}
