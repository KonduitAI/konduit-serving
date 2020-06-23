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

/**
 * VideoFrameCaptureStep extracts a single frame from a video each time inference is called.
 * The video path is hardcoded
 *
 * Note that at present this makes it only practically useful for testing/demo purposes.
 * Other options for loading the video will be specified at a later date: https://github.com/KonduitAI/konduit-serving/issues/350
 */
@Data
@Accessors(fluent=true)
@NoArgsConstructor
@JsonName("VIDEO_CAPTURE")
@Schema(description = "A pipeline step that configures how to extracts a single frame from a video each time inference is called." +
        " The video path is hardcoded, mainly used for testing/demo purposes given this")
public class VideoFrameCaptureStep implements PipelineStep {

    @Schema(description = "Location of the video file.")
    private String filePath;

    
    @Schema(description = "Name of the output key where the image frame will be located.",
            defaultValue = "image")
    private String outputKey = "image";

    
    @Schema(description = "Loop the video when it reaches the end?")
    private boolean loop = true;

    public VideoFrameCaptureStep(@JsonProperty("filePath") String filePath, @JsonProperty("outputKey") String outputKey, @JsonProperty("loop") boolean loop){
        this.filePath = filePath;
        this.outputKey = outputKey;
        this.loop = loop;
    }

}
