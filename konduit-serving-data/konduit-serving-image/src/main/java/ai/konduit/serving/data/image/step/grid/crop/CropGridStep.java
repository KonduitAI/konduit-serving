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
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Crop sub images out of a larger image, based on a grid. The grid location is defined in terms of the x/y coordinates
 * of the corners (which comes from the input Data instance), and the number of segments within the grid in both directions.<br>
 * The 4 corner X coordinates come from {@code Data.getListDouble(xName)} and the 4 corner Y coordinates come from
 * {@code Data.getListDouble(yName)}.<br>
 * Note that the output depends on the configuration.
 * Always returned: {@code List<Image>} - the cropped images from the grid<br>
 * Returned if {@code outputCoordinates=true}: two {@code List<Long>}s - the box coordinates (0,0), ..., (grid1-1, grid2-1)<br>
 * Returned if {@code boundingBoxName != null}: one {@code List<BoundingBox>} - the crop bounding boxes, (0,0), (0,1), ..., (grid1-1, grid2-1)<br>
 * See also {@link CropFixedGridStep}<br>
 * If {@code aspectRatio} is set, the smaller dimension will be increased to keep the aspect ratio correct. Note this may crop
 * outside the image border
 * @author Alex Black
 * @see CropFixedGridStep
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("CROP_GRID")
@Schema(description = "A pipeline step that crops sub images out of a larger image, based on a grid. " +
        "The 4 corner coordinates are defined as points, and come from {@code Data.getListPoint(name)}, in the following order:" +
        "topLeft, topRight, bottomLeft, bottomRight<br>" +
        "gridX is the number of grid segments between (topLeft and topRight) and (bottomLeft and bottomRight).<br>" +
        "gridY is the number of grid segments between (topLeft and bottomLeft) and (topRight and bottomRight)<br>" +
        "The output contains a List<Image> of cropped images from the grid.")
public class CropGridStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "crops";

    @Schema(description = "Name of the input image key from the previous step. If set to null, it will try to find any image in the incoming data instance.")
    private String imageName;

    @Schema(description = "Name of the List<Point> points specifying the corners, in order: topLeft, topRight, bottomLeft, bottomRight")
    private String pointsName;

    @Schema(description = "The number of grid segments between (topLeft and topRight) and (bottomLeft and bottomRight)")
    private int gridX;

    @Schema(description = "The number of grid segments between (topLeft and bottomLeft) and (topRight and bottomRight)")
    private int gridY;

    @Schema(description = "If true, the lists are in pixels coordinates, not from 0 to 1.")
    private boolean coordsArePixels;

    @Schema(description = "Name of the output bounding boxes key.")
    private String boundingBoxName;

    @Schema(description = "If true, the two lists are returned which contains the data of grid horizontal and verticle coordinates, respectively.")
    private boolean outputCoordinates;

    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.")
    private boolean keepOtherFields;

    @Schema(description = "If set, the smaller dimensions will be increased to keep the aspect ratio correct (which may crop outside the image border).")
    private Double aspectRatio;

    @Schema(description = "Name of the key of all the cropped output images from this step.",
            defaultValue = DEFAULT_OUTPUT_NAME)
    private String outputName;
}
