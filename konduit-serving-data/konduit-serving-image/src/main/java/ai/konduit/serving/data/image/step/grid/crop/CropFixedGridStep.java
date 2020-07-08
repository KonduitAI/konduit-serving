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

package ai.konduit.serving.data.image.step.grid.crop;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.List;

/**
 * As per {@link CropGridStep} but the x/y location values are hardcoded into the configuration, instead of coming
 * dynamically from the input Data instance
 *
 * @author Alex Black
 * @see CropGridStep
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("CROP_FIXED_GRID")
@Schema(description = "This step is similar to the CropGridStep with the difference that the x/y location values are " +
        "hardcoded into the configuration, instead of coming dynamically from the input Data instance.")
public class CropFixedGridStep implements PipelineStep {

    @Schema(description = "Name of the input image key from the previous step. If set to null, it will try to find any image in the incoming data instance.")
    private String imageName;

    @Schema(description = "A List<Point> (of length 4), the corners, in order: topLeft, topRight, bottomLeft, bottomRight")
    private List<Point> points;

    @Schema(description = "The number of grid segments between (topLeft and topRight) and (bottomLeft and bottomRight)")
    private int gridX;

    @Schema(description = "The number of grid segments between (topLeft and bottomLeft) and (topRight and bottomRight)")
    private int gridY;

    @Schema(description = "If true, the points are in pixels coordinates (0 to width-1) and (0 to height-1); if false, they " +
            "are 0.0 to 1.0 (fraction of image height/width)")
    private boolean coordsArePixels;

    @Schema(description = "Name of the output bounding boxes key.")
    private String boundingBoxName;

    @Schema(description = "If true, the two lists are returned which contains the data of grid horizontal and verticle coordinates, respectively.")
    private boolean outputCoordinates;

    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.")
    private boolean keepOtherFields;

    @Schema(description = "If set, the smaller dimensions will be increased to keep the aspect ratio correct (which may crop outside the image border).")
    private Double aspectRatio;

    @Schema(description = "Name of the key of all the cropped output images from this step.")
    private String outputName;

    @Tolerate
    public CropFixedGridStep points(Point... points) {
        return this.points(Arrays.asList(points));
    }
}
