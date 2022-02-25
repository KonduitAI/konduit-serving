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

import ai.konduit.serving.pipeline.util.ObjectMappers;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Class to represent the error response details when the inference HTTP API fails at a certain point.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "An object specifying error information for anything that doesn't happen according to plan while " +
        "sending inference requests to the konduit serving http server.")
public class ErrorResponse implements Serializable {

    @Schema(description = "Error code associated with the error object.")
    private HttpApiErrorCode errorCode;

    @Schema(description = "The message associated with the error.")
    private String errorMessage;

    public static ErrorResponse fromJson(String jsonString) {
        return ObjectMappers.fromJson(jsonString, ErrorResponse.class);
    }
}
