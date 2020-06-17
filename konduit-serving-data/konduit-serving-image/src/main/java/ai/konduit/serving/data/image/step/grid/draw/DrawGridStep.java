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
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonName("DRAW_GRID")
@Schema(description = "A pipeline step that configures how a grid is drawn based on the input taken from a " +
        "CropGridStep. This step draws a grid on the specified image, based on the x/y coordinates of the corners, " +
        "and the number of segments within the grid in both directions. The 4 corner X coordinates come from a double array in <xName> " +
        "and the 4 corner Y coordinates come from a double array in <yName>. The order of the the X/Y coordinates does not matter, " +
        "other than grid1 value corresponding to the number of segments between the (xName[0],yName[0]) and (xName[1],yName[1]) corners.")
public class DrawGridStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

    @Schema(description = "Name of the input image key from the previous step. If set to null, it will try to find any image in the incoming data instance.")
    private String imageName;

    @Schema(description = "Name of the key of a list (of length 4), specifying X coordinates in any order.")
    private String xName;

    @Schema(description = "Name of the key of a list (of length 4), specifying Y coordinates in any order (that matches X order).")
    private String yName;

    @Schema(description = "Number of grid segments between (xName[0],yName[0]) and (xName[1],yName[1]).")
    private int grid1;

    @Schema(description = "Number of grid segments in the other direction (between (xName[2],yName[2]) and (xName[3],yName[3])).")
    private int grid2;

    @Schema(description = "If true, the lists are in pixels coordinates, not from 0 to 1.")
    private boolean coordsArePixels;

    @Schema(description = "Color of the border. The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private String borderColor;

    @Schema(description = "Color of the grid. The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private String gridColor;

    @Builder.Default
    @Schema(description = "Line thickness to use to draw the border (in pixels).",
            defaultValue = "1")
    private int borderThickness = 1;

    @Schema(description = "Line thickness to use to draw the border (in pixels). " +
            "If null then the same value as the borderThickness is used")
    private Integer gridThickness;

    public DrawGridStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null or 0 etc
        this.borderThickness = 1;
    }

}
