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

package ai.konduit.serving.data.image.step.point.heatmap;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Draw a heatmap using 2D points.<br>
 * Heat will accumulate over the lifecycle of this step.<br>
 * Configuration:<br>
 * <ul>
 *     <li><b>points</b>: Names of the points to be used for the heatmap. Accepts both single points and lists of points.
 *     <li><b>radius</b>: Size of area influenced by a point.</li>
 *     <li><b>fadingFactor</b>: Optional. Value between 0 and 1. 0: No Fade, 1: Instant fade; default: 0.9</li>
 *     <li><b>image</b>: Optional. Name of the image to be drawn on</li>
 *     <li><b>opacity</b>: Optional. Opacity of the heatmap. Between 0 and 1. 0: Fully transparent, 1: Fully opaque; default: 0.5</li>
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
@JsonName("DRAW_HEATMAP")
@Schema(description = "A pipeline step that configures how to draw a 2D heatmap on an image.")
public class DrawHeatmapStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "image";

    @Schema(description = "Name of the input data fields containing the points used for the heatmap. Accepts both single points and lists of points. Accepts both relative and absolute addressed points.")
    @Singular
    private List<String> points;

    @Schema(description = "Size of area influenced by a point")
    private Integer radius;

    @Schema(description = "Fading factor. 0: no fade, 1: instant fade")
    private Double fadingFactor;

    @Schema(description = "An optional field, specifying the name of the image to draw on")
    private String image;

    @Schema(description = "Opacity of the heatmap. Between 0 and 1. 0: Fully transparent, 1: Fully opaque. Default: 0.5")
    private Double opacity;

    @Schema(description = "Must be provided when \"image\" isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)")
    private Integer width;

    @Schema(description = "Must be provided when \"image\" isn't set. Used to resolve position of points with relative addressing (dimensions between 0 and 1)")
    private Integer height;

    @Schema(description = "Name of the output image",
            defaultValue = DEFAULT_OUTPUT_NAME)
    private String outputName;
}
