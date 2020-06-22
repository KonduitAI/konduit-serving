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
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Draw a grid on the specified image, based on the x/y coordinates of the corners, and the number of segments within
 * the grid in both directions.<br>
 * The 4 corner coordinates are defined as points, and come from {@code Data.getListPoint(name)}, in the following order:
 * topLeft, topRight, bottomLeft, bottomRight<br>
 * gridX is the number of grid segments between (topLeft and topRight) and (bottomLeft and bottomRight).<br>
 * gridY is the number of grid segments between (topLeft and bottomLeft) and (topRight and bottomRight)<br>
 * The colors and line thicknesses can be configured.
 * <p>
 * See also {@link DrawFixedGridStep}
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("DRAW_GRID")
@Schema(description = "Draw a grid on the specified image, based on the x/y coordinates of the corners, and the number of segments within " +
        "the grid in both directions.<br>" +
        "The 4 corner coordinates are defined as points, and come from {@code Data.getListPoint(name)}, in the following order:" +
        "topLeft, topRight, bottomLeft, bottomRight<br>" +
        "gridX is the number of grid segments between (topLeft and topRight) and (bottomLeft and bottomRight).<br>" +
        "gridY is the number of grid segments between (topLeft and bottomLeft) and (topRight and bottomRight)<br>" +
        "The colors and line thicknesses can be configured.")
public class DrawGridStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

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



}
