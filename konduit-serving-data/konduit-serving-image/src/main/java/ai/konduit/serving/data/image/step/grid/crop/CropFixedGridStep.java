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
@JsonName(jsonName = "CROP_FIXED_GRID", subclassOf = PipelineStep.class)
public class CropFixedGridStep implements PipelineStep {
    private String imageName;               //If null: just find any image
    private double[] x;                     //length 4, specifying X coordinates in any order
    private double[] y;                     //length 4, specifying Y coordinates in any order (that matches X order)
    private int grid1;                      //Number of grid segments between (x[0],y[0]) and (x[1],y[1])
    private int grid2;                      //Number of grid segments in the other direction
    private boolean coordsArePixels;        //If true: Lists are in pixels, not 0 to 1
    private String boundingBoxName;
    private boolean outputCoordinates;
    private boolean keepOtherFields;
    private Double aspectRatio;
    private String outputName;
}
