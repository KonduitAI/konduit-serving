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

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.Builder;
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * VideoFrameCaptureStep extracts a single frame from a video each time inference is called.
 * The video path is hardcoded
 *
 * Note that at present this makes it only practically useful for testing/demo purposes.
 * Other options for loading the video will be specified at a later date: https://github.com/KonduitAI/konduit-serving/issues/350
 */
@Builder
@Data
public class VideoFrameCaptureStep implements PipelineStep {

    private String filePath;
    @Builder.Default
    private String outputKey = "image";

    public VideoFrameCaptureStep(@JsonProperty("filePath") String filePath, @JsonProperty("outputKey") String outputKey){
        this.filePath = filePath;
        this.outputKey = outputKey;
    }

}
