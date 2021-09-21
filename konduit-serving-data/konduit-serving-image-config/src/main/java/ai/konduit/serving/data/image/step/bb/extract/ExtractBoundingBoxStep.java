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

package ai.konduit.serving.data.image.step.bb.extract;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 *
 * ExtractBoundingBoxStep: Given one or more bounding boxes, and an input image, extract from the input region an image
 * that corresponds to the bounding box region.<br>
 * i.e.: output as images the input image region covered by the bounding boxes<br>
 * Note: supports both {@code BoundingBox} and {@code List<BoundingBox>} fields. If the input is as single value,
 * the output will be a single value; if the input is a list, the output will be a list.<br>
 * <br>
 * Note: If the aspect ratio field is set, the image cropping will increase the smaller dimension to ensure the cropped
 * image complies with the requested aspect ratio.<br>
 * <br>
 * Note: If resizeH and resizeW are specified, the cropped images will be resized to the specified size
 *
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("EXTRACT_BOUNDING_BOX")
@Schema(description = "A pipeline step that extracts sub-images from an input image, based on the locations of input bounding boxes. " +
        "Returns List<Image> for the cropped image regions")
public class ExtractBoundingBoxStep implements PipelineStep {

    @Schema(description = "Name of the input image key from the previous step. If set to null, it will try to find any image in the incoming data instance.")
    private String imageName;

    @Schema(description = "Name of the bounding boxes key from the previous step. If set to null, it will try to find any bounding box in the incoming data instance.")
    private String bboxName;

    @Schema(description = "Name of the output key that will contain the output as images the input image " +
            "region covered by the bounding boxes.")
    private String outputName;

    
    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.",
            defaultValue = "true")
    private boolean keepOtherFields = true;

    @Schema(description = "If set, the smaller dimensions will be increased to keep the aspect ratio correct (which may crop outside the image border).")
    private Double aspectRatio = null;

    @Schema(description = "If specified, the cropped images will be resized to the specified height.")
    private Integer resizeH;

    @Schema(description = "If specified, the cropped images will be resized to the specified width.")
    private Integer resizeW;

    @Schema(description = "Used to account for the fact that n-dimensional array from ImageToNDArrayConfig may be " +
            "used to crop images before passing to the network, when the image aspect ratio doesn't match the NDArray " +
            "aspect ratio. This allows the step to determine the subset of the image actually passed to the network that " +
            "produced the bounding boxes.")
    private ImageToNDArrayConfig imageToNDArrayConfig;


}
