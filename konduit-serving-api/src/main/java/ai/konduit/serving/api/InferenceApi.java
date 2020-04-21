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
import ai.konduit.serving.config.Input;
import ai.konduit.serving.config.Output;
import ai.konduit.serving.output.types.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class InferenceApi {

    @GET
    @Path("/config")
    public InferenceConfiguration getConfig() {
        return new InferenceConfiguration();
    }

    @POST
    @Path("/{predictionType}/{inputDataFormat}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Get inference result with multipart data.",
            tags = {"inference"},
            description = "You can send multipart data for inference where the input names will be the names of the inputs of a transformation process. " +
                    "or a model input and the corresponding files containing the data for each input.",
            responses = {
                    @ApiResponse(description = "Batch output data",
                            responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ClassifierOutput.class,
                                    RegressionOutput.class,
                                    DetectedObjectsBatch.class,
                                    ManyDetectedObjects.class
                            }))
                    ),
            }
    )
    public BatchOutput predict(@PathParam("predictionType") Output.PredictionType predictionType,
                               @PathParam("inputDataFormat") Input.DataFormat inputDataFormat,
                               @Parameter(description = "An array of files to upload.") File[] multipartInput) {
        return new ClassifierOutput();
    }
}