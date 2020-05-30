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

package ai.konduit.serving.data.image.step.grid.draw;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Draw a grid on the specified image, based on the x/y coordinates of the corners, and the number of segments within
 * the grid in both directions.<br>
 * The 4 corner X coordinates come from {@code Data.getListDouble(xName)} and the 4 corner Y coordinates come from
 * {@code Data.getListDouble(yName)}.<br>
 * Note 1: The order of the the X/Y coordinates does not matter, other than grid1 value corresponding to the number of
 * segments between the (x[0],y[0]) and (x[1],y[1]) corners.<br>
 * The colors and line thicknesses can be configured.
 * <p>
 * See also {@link DrawFixedGridStep}
 *
 * @author Alex Black
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName(jsonName = "DRAW_GRID", subclassOf = PipelineStep.class)
public class DrawGridStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

    private String imageName;               //If null: just find any image
    private String xName;                   //Name of the List<Long> or List<Double> of length 4, specifying X coordinates in any order
    private String yName;                   //Name of the List<Long> or List<Double> of length 4, specifying Y coordinates in any order (that matches X order)
    private int grid1;                      //Number of grid segments between (x[0],y[0]) and (x[1],y[1])
    private int grid2;                      //Number of grid segments in the other direction
    private boolean coordsArePixels;        //If true: Lists are in pixels, not 0 to 1
    private String borderColor;
    private String gridColor;
    @Builder.Default
    private int borderThickness = 1;
    private Integer gridThickness;          //If null: same thickness as border

    public DrawGridStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null or 0 etc
        this.borderThickness = 1;
    }

}
