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
import ai.konduit.serving.data.image.step.grid.crop.CropFixedGridStep;
import ai.konduit.serving.data.image.util.ColorUtil;
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
 * As per {@link DrawGridStep} but the x/y location values are hardcoded into the configuration, instead of coming
 * dynamically from the input Data instance
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("DRAW_FIXED_GRID")
@Schema(description = "A pipeline step that draws a grid on an image. This is similar to DrawGridStep but the corner x/y" +
        " location values are hardcoded into the configuration (via points), instead of coming dynamically from the input Data instance.")
public class DrawFixedGridStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

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

    @Schema(description = "Color of the border. " + ColorUtil.COLOR_DESCRIPTION)
    private String borderColor;

    @Schema(description = "Color of the grid. If not set, the border color will be used. " + ColorUtil.COLOR_DESCRIPTION)
    private String gridColor;

    @Schema(description = "Line thickness to use to draw the border (in pixels).",
            defaultValue = "1")
    private int borderThickness = 1;

    @Schema(description = "Line thickness to use to draw the border (in pixels). " +
            "If null then the same value as the borderThickness is used")
    private Integer gridThickness;

    @Tolerate
    public DrawFixedGridStep points(Point... points) {
        return this.points(Arrays.asList(points));
    }


}
