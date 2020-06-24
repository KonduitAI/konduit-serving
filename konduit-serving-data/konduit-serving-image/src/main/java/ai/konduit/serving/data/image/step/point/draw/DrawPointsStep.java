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

package ai.konduit.serving.data.image.step.point.draw;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Draw 2D points<br>
 * Configuration:<br>
 * <ul>
 *     <li><b>classColors</b>: Optional: A list of colors to use for each class. Must be in one of the following formats: hex/HTML - "#788E87",
 *             RGB - "rgb(128,0,255)", or one of 16 HTML color name such as \"green\" (https://en.wikipedia.org/wiki/Web_colors#HTML_color_names).
 *             If no colors are specified, or not enough colors are specified, random colors are used instead (note consistent between runs).
 *             Colors are mapped to named classes in alphabetical order, i.e. first color to class A, second color to class B, etc...</li>
 *     <li><b>points</b>: Names of the points to be drawn. Accepts both single points and lists of points.
 *     <li><b>radius</b>: Point radius on drawn image. </li>
 *     <li><b>image</b>: Optional. Name of the image to use as size reference</li>
 *     <li><b>width</b>: Must be provided when <b>image</b> isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)</li>
 *     <li><b>height</b>: Must be provided when <b>image</b> isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)</li>
 *     <li><b>outputName</b>: Name of the output image</li>
 * </ul>
 *
 * @author Paul Dubs
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("DRAW_POINTS")
@Schema(description = "A pipeline step that configures how to draw 2D points.")
public class DrawPointsStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "image";

    @Schema(description = "This is an optional field which specifies the mapping of colors to use for each class. " +
            "The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private Map<String, String> classColors;

    @Schema(description = "Name of the input data fields containing the points to be drawn. Accepts both single points and lists of points. Accepts both relative and absolute addressed points.")
    @Singular
    private List<String> points;

    @Schema(description = "Optional. Point radius on drawn image. Default = 5px")
    private Integer radius;

    @Schema(description = "An optional field, specifying the name of the image to use as size reference")
    private String image;

    @Schema(description = "Must be provided when \"image\" isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)")
    private Integer width;

    @Schema(description = "Must be provided when \"image\" isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)")
    private Integer height;



    @Schema(description = "Name of the output image",
            defaultValue = DEFAULT_OUTPUT_NAME)
    private String outputName;
}
