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
@JsonName(jsonName = "CROP_GRID", subclassOf = PipelineStep.class)
public class CropGridStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "crops";

    private String imageName;               //If null: just find any image
    private String xName;                   //Name of the List<Long> or List<Double> of length 4, specifying X coordinates in any order
    private String yName;                   //Name of the List<Long> or List<Double> of length 4, specifying Y coordinates in any order (that matches X order)
    private int grid1;                      //Number of grid segments between (x[0],y[0]) and (x[1],y[1])
    private int grid2;                      //Number of grid segments in the other direction
    private boolean coordsArePixels;        //If true: Lists are in pixels, not 0 to 1
    private String boundingBoxName;
    private boolean outputCoordinates;
    private boolean keepOtherFields;
    private Double aspectRatio;
    private String outputName;
}
