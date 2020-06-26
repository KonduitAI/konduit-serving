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

package ai.konduit.serving.data.image.step.segmentation.index;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
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
 * Draw segmentation mask, optionally on an image.<br>
 * Configuration:<br>
 * <ul>
 *     <li><b>classColors</b>: Optional: A list of colors to use for each class. Must be in one of the following formats: hex/HTML - "#788E87",
 *             RGB - "rgb(128,0,255)", or one of 16 HTML color name such as \"green\" (https://en.wikipedia.org/wiki/Web_colors#HTML_color_names).
 *             If no colors are specified, or not enough colors are specified, random colors are used instead (note consistent between runs)</li>
 *     <li><b>segmentArray</b>: Name of the NDArray with the class indices, 0 to numClasses-1. Shape [1, height, width]</li>
 *     <li><b>image</b>: Optional. Name of the image to draw the segmentation classes on to. If not provided, the segmentation classes are drawn
 *     onto a black background image</li>
 *     <li><b>outputName</b>: Name of the output image</li>
 *     <li><b>opacity</b>: Optional. Only used when "image" configuration is set. The opacity, between 0.0 and 1.0, of the mask to draw on the image.
 *     Default value of 0.5 if not set. Value of 0.0 is fully transparent, 1.0 is fully opaque.</li>
 *     <li><b>backgroundClass</b>: Optional. If set: Don't draw this class. If not set: all classes will be drawn</li>
 * </ul>
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("DRAW_SEGMENTATION")
@Schema(description = "A pipeline step that configures how to draw a segmentation mask, optionally on an image.")
public class DrawSegmentationStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "image";
    public static final double DEFAULT_OPACITY = 0.5;

    @Schema(description = "This is an optional field which specifies the list of colors to use for each class. " +
            "The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private List<String> classColors;

    @Tolerate
    public DrawSegmentationStep classColors(String... classColors){
        return this.classColors(Arrays.asList(classColors));
    }

    @Schema(description = "Name of the NDArray with the class indices, 0 to numClasses-1. Shape [1, height, width].")
    private String segmentArray;

    @Schema(description = "An optional field, specifying the name of the image to draw the segmentation classes " +
            "on to. If not provided, the segmentation classes are drawn onto a black background image.")
    private String image;

    @Schema(description = "Name of the output image",
            defaultValue = DEFAULT_OUTPUT_NAME)
    private String outputName;

    @Schema(description = "An optional field, that is used only used when <image> key name is set. This specifies, the opacity, " +
            "between 0.0 and 1.0, of the mask to draw on the image. Default value of 0.5 is used if it's not set. " +
            "Value of 0.0 is fully transparent, 1.0 is fully opaque.", defaultValue = "0.5")
    private Double opacity;

    @Schema(description = "An optional field, specifying a class that's not to be drawn. If not set, all classes will be drawn")
    private Integer backgroundClass;

    @Schema(description = "Used to account for the fact that n-dimensional array from ImageToNDArrayConfig may be " +
            "used to crop images before passing to the network, when the image aspect ratio doesn't match the NDArray " +
            "aspect ratio. This allows the step to determine the subset of the image actually passed to the network that " +
            "produced the segmentation prediction to be drawn.")
    private ImageToNDArrayConfig imageToNDArrayConfig;

}
