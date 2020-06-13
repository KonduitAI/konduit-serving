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

package ai.konduit.serving.data.image.step.face;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A step for drawing face keypoints.
 * Assumes the key point array is an NDArray with shape [numExamples][2*numPoints], where each entry alternates x and y
 * coordinates, in 0 to 1 scale. Other formats may be added in the future.
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class DrawFaceKeyPointsStep implements PipelineStep {
    public static final String DEFAULT_BOX_COLOR = "lime";
    public static final String DEFAULT_POINT_COLOR = "red";
    public static final String DEFAULT_OUTPUT_NAME = "image";

    public enum Scale {NONE, AT_LEAST, AT_MOST}

    private ImageToNDArrayConfig imageToNDArrayConfig;
    private int resizeH;
    private int resizeW;
    @Builder.Default
    private boolean drawFaceBox = true;
    private String faceBoxColor;
    private String pointColor;
    @Builder.Default
    private int pointSize = 1;


    @Builder.Default
    private Scale scale = Scale.NONE;

    private String landmarkArray;
    private String image;
    private String outputName;

    public DrawFaceKeyPointsStep() {
        this.scale = Scale.NONE;
        this.pointSize = 1;
        this.drawFaceBox = true;
    }
}


