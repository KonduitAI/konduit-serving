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

import ai.konduit.serving.annotation.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

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
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName(jsonName = "DRAW_SEGMENTATION", subclassOf = PipelineStep.class)
public class DrawSegmentationStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "image";
    public static final double DEFAULT_OPACITY = 0.5;

    private List<String> classColors;
    private String segmentArray;
    private String image;
    private String outputName;
    private Double opacity;         //0 to 1
    private Integer backgroundClass;
    private ImageToNDArrayConfig imageToNDArrayConfig;

}
