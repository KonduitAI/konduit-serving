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

package ai.konduit.serving.data.image.step.point.perspective.convert;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.step.point.heatmap.DrawHeatmapStep;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.List;

/**
 * PerspectiveTransformStep: Applies a perspective transformation to Images, Points and Bounding Boxes.<br>
 * The transformation is defined either statically with the Step definition or dynamically with a list of 4 points in an
 * input. If both are defined, an IllegalStateException will be thrown.<br>
 * If only the source points are defined, the transformation will result in mapping those points to a rectangle. <br>
 * If you want to apply a more specific transformation, you can also provide the target points yourself.<br>
 * Note: supports both single values and lists. If the input is as single value,
 * the output will be a single value; if the input is a list, the output will be a list.<br>
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("PERSPECTIVE_TRANSFORM")
public class PerspectiveTransformStep implements PipelineStep {
    /**
     * If null: just find any Points, Bounding Boxes and Images
     */
    @Singular
    private List<String> inputNames;

    @Tolerate
    public PerspectiveTransformStep inputNames(String... inputNames){
        return this.inputNames(Arrays.asList(inputNames));
    }

    @Singular
    private List<String> outputNames;

    @Tolerate
    public PerspectiveTransformStep outputNames(String... outputNames){
        return this.outputNames(Arrays.asList(outputNames));
    }

    /**
     * When you provide source points as an input, they must be provided as a list of 4 points [topLeft, topRight, bottomLeft, bottomRight]
     */
    private String sourcePointsName;
    /**
     * When you provide target points as an input, they must be provided as a list of 4 points [topLeft, topRight, bottomLeft, bottomRight]
     */
    private String targetPointsName;

    /**
     * When a referenceImage is provided, the transform will be adjusted to ensure the entire transformed image fits into the output image (up to 4096x4096)<br>
     * Can also reference a list of images, in which case <b>only the first</b> image is used as the reference.<br>
     * The adjustment is applied to all inputs of the step.
     */
    private String referenceImage;

    /**
     * When a reference width and height are provided, the transform will be adjusted to make sure the entire area fits into the output image (up to 4096x4096)<br>
     * The adjustment is applied to all inputs of the step.
     */
    private Integer referenceWidth;
    /**
     * When a reference width and height are provided, the transform will be adjusted to make sure the entire area fits into the output image (up to 4096x4096)<br>
     * The adjustment is applied to all inputs of the step.
     */
    private Integer referenceHeight;


    /**
     * takes exactly 4 points [topLeft, topRight, bottomLeft, bottomRight]
     */
    private List<Point> sourcePoints;
    /**
     * takes exactly 4 points [topLeft, topRight, bottomLeft, bottomRight]
     */
    private List<Point> targetPoints;


    private boolean keepOtherFields = true;

}
