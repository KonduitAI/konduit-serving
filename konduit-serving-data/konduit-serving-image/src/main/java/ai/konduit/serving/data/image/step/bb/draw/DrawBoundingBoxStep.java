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

package ai.konduit.serving.data.image.step.bb.draw;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 *
 * Scale:
 * <ul>
 *     <li>NONE: No scaling</li>
 *     <li>AT_LEAST: Scale up if necessary, so H >= scaleH and W >= scaleW</li>
 *     <li>AT_MOST: Scale down if necessary, so H <= scaleH and W <= scaleW</li>
 * </ul>
 *
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName("DRAW_BOUNDING_BOX")
@Schema(description = "A pipeline step that configures how to draw a bounding box onto an image. The bounding box data, that's to " +
        "be drawn, is taken from the previous step's data instance.")
public class DrawBoundingBoxStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

    @Schema(description = "A scaling policy enum, specifying how to scale the bounding box width and height. " +
            "NONE -> No scaling, AT_LEAST -> Scale up if necessary, so H >= scaleH and W >= scaleW, AT_MOST -> " +
            "Scale down if necessary, so H <= scaleH and W <= scaleW")
    public enum Scale {
        NONE,
        AT_LEAST,
        AT_MOST
    }

    @Schema(description = "Name of the input image key from the previous step. If set to null, it will try to find any image in the incoming data instance.")
    private String imageName;

    @Schema(description = "Name of the bounding boxes key from the previous step. If set to null, it will try to find any bounding box in the incoming data instance.")
    private String bboxName;

    @Schema(description = "If true, then draw the class label on top of the bounding box.")
    private boolean drawLabel;

    @Schema(description = "If true, then draw the class probability on top of the bounding box.")
    private boolean drawProbability;

    @Schema(description = "Specifies the color of different classes/labels that are drawn. The color can be a hex/HTML string like" +
            "\"#788E87\", an RGB value like RGB - \"rgb(128,0,255)\" or  it can be from a set of predefined HTML color names: " +
            "[white, silver, gray, black, red, maroon, yellow, olive, lime, green, aqua, teal, blue, navy, fuchsia, purple]")
    private Map<String, String> classColors;

    @Schema(description = "The default color to use in case a color for a label/class is not defined.",
            defaultValue = DEFAULT_COLOR)
    private String color;

    @Builder.Default
    @Schema(description = "Line thickness to use to draw the bounding box (in pixels).",
            defaultValue = "1")
    private int lineThickness = 1;

    @Builder.Default
    @Schema(description = "The scaling policy to use for scaling the bounding boxes.",
            defaultValue = "NONE")
    private Scale scale = Scale.NONE;

    @Schema(description = "Height threshold to be used with the scaling policy.")
    private int resizeH;

    @Schema(description = "Width threshold to be used with the scaling policy.")
    private int resizeW;

    @Schema(description = "Used to account for the fact that n-dimensional array from ImageToNDArrayConfig may be " +
            "used to crop images before passing to the network, when the image aspect ratio doesn't match the NDArray " +
            "aspect ratio. This allows the step to determine the subset of the image actually passed to the network.")
    private ImageToNDArrayConfig imageToNDArrayConfig;

    @Schema(description = "If true, the cropped region based on the image array is drawn.", defaultValue = "false")
    private boolean drawCropRegion = false;

    @Schema(description = "Color of the crop region. Only used if drawCropRegion = true.")
    private String cropRegionColor;

    /*
    Other things could add:
    - Upscale? (or minimum resolution, or always scale) - also aspect ratio part...
    - Text size
    - Text font
    - Line width
     */

    public DrawBoundingBoxStep(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.scale = Scale.NONE;
        this.lineThickness = 1;
    }

}
