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
 * As per {@link CropGridStep} but the x/y location values are hardcoded into the configuration, instead of coming
 * dynamically from the input Data instance
 *
 * @author Alex Black
 * @see CropGridStep
 */
@Builder
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

    @Schema(description = "A list (of length 4), specifying X coordinates in any order.")
    private double[] x;

    @Schema(description = "A list (of length 4), specifying Y coordinates in any order (that matches X order).")
    private double[] y;

    @Schema(description = "Number of grid segments between (x[0],y[0]) and (x[1],y[1]).")
    private int grid1;

    @Schema(description = "Number of grid segments in the other direction (between (x[2],y[2]) and (x[3],y[3])).")
    private int grid2;

    @Schema(description = "If true, the x/y coordinate lists/arrays are in pixels coordinates, not from 0 to 1.")
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
}
