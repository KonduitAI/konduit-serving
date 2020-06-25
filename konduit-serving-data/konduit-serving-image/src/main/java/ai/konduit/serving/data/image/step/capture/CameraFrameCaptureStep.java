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
package ai.konduit.serving.data.image.step.capture;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

// TODO: not sure if it's relevant here but maybe we can later add a variable to specify the frames per second
//  or a way to specify max number of inferences per second. This can potentially save us some compute.- SHAMS
@Data
@Accessors(fluent=true)
@NoArgsConstructor
@JsonName("FRAME_CAPTURE")
@Schema(description = "A pipeline step that specifies an input that's taken from a camera feed.")
public class CameraFrameCaptureStep implements PipelineStep {

    
    @Schema(description = "ID of the camera from which the input is taken from. Each system cameras is assigned an ID, " +
            "which is usually 0 for the first device, 1 for the second and so on...",
            defaultValue = "0")
    private int camera = 0;         //TODO add other (more robust) ways to select camera

    
    @Schema(description = "Width of the incoming image frame. This will scale the original resolution width to the specified value.",
            defaultValue = "640")
    private int width = 640;

    
    @Schema(description = "Height of the incoming image frame. This will scale the original resolution height to the specified value.",
            defaultValue = "480")
    private int height = 480;

    
    @Schema(description = "Name of the output key that will contain and carry the image frame data to the later pipeline steps.",
            defaultValue = "image")
    private String outputKey = "image";

    public CameraFrameCaptureStep(@JsonProperty("camera") int camera, @JsonProperty("width") int width,
                                  @JsonProperty("height") int height, @JsonProperty("outputKey") String outputKey){
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.outputKey = outputKey;
    }

}
