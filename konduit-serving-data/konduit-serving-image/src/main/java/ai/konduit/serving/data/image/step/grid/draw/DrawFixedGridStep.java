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
 * As per {@link DrawGridStep} but the x/y location values are hardcoded into the configuration, instead of coming
 * dynamically from the input Data instance
 *
 * @author Alex Black
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName("DRAW_FIXED_GRID")
@Schema(description = "A pipeline step that configures how a grid is drawn based on the input taken from a " +
        "CropFixedGridStep. This is similar to DrawGridStep but the x/y location values are hardcoded into " +
        "the configuration, instead of coming dynamically from the input Data instance.")
public class DrawFixedGridStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

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

    @Schema(description = "If true, the lists are in pixels coordinates, not from 0 to 1.")
    private boolean coordsArePixels;

    @Schema(description = "Color of the border. If not setThe color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private String borderColor;

    @Schema(description = "Color of the grid. If not set, the border color will be used. The color can be a hex/HTML string like" +
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

    public DrawFixedGridStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null or 0 etc
        this.borderThickness = 1;
    }

}
