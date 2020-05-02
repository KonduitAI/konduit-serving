/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.api;

import ai.konduit.serving.InferenceConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static ai.konduit.serving.api.ApiUtils.sendGeneralResponse;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Data
@SuperBuilder
public class CommonApi {

    InferenceConfiguration inferenceConfiguration;

    @GET
    @Path("/config")
    @Operation(
            summary = "Get the inference configuration associated with the server.",
            tags = {"config"},
            description = "Inference configuration JSON that's associated with the server.",
            responses = {
                    @ApiResponse(description = "Inference configuration as json",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = InferenceConfiguration.class))
                    ),
                    @ApiResponse(description = "Internal server error while handling the request.",
                            responseCode = "500"
                    )
            }
    )
    public Response getConfig() {
        return sendGeneralResponse(inferenceConfiguration.toJsonObject().encode());
    }

    @GET
    @Path("/config/pretty")
    @Operation(
            summary = "Get the inference configuration associated with the server in indented format.",
            tags = {"config"},
            description = "Inference configuration JSON that's associated with the server in indented format.",
            responses = {
                    @ApiResponse(description = "Inference configuration as indented json",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = InferenceConfiguration.class))
                    ),
                    @ApiResponse(description = "Internal server error while handling the request.",
                            responseCode = "500"
                    )
            }
    )
    public Response getConfigPretty() {
        return sendGeneralResponse(inferenceConfiguration.toJsonObject().encodePrettily());
    }
}