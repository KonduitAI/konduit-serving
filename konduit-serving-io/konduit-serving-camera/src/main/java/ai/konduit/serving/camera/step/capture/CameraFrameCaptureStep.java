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
package ai.konduit.serving.camera.step.capture;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

@Builder
@Data
@JsonName("FRAME_CAPTURE")
public class CameraFrameCaptureStep implements PipelineStep {

    @Builder.Default
    private int camera = 0;         //TODO add other (more robust) ways to select camera
    @Builder.Default
    private int width = 640;
    @Builder.Default
    private int height = 480;
    @Builder.Default
    private String outputKey = "image";

    public CameraFrameCaptureStep(@JsonProperty("camera") int camera, @JsonProperty("width") int width,
                                  @JsonProperty("height") int height, @JsonProperty("outputKey") String outputKey){
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.outputKey = outputKey;
    }

}
