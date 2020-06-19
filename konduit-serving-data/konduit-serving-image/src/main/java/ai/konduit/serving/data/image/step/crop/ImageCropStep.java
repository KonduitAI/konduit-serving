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

package ai.konduit.serving.data.image.step.crop;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.config.AspectRatioHandling;
import ai.konduit.serving.pipeline.api.data.BoundingBox;
import ai.konduit.serving.pipeline.api.data.Point;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;


/**
 * Crop an image to the specified rectangular region. The crop region may be specified in one of two ways:<br>
 * (a) Via a bounding box, or<br>
 * (b) Via a {@code List<Point>} of length 2, containing the top-left and bottom-right crop locations.<br>
 * Furthermore, the bounding box and corner point coordinates may be specified in terms of either pixels or
 * "fraction of image". Note that if the crop region falls partly outside the input image region, black padding
 * will be added as necessary to keep the requested output size.<br>
 * Supports both {@code Image} and {@code List<Image>} inputs.
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@JsonName("RESIZE")
@NoArgsConstructor
@Schema(description = "Crop an image to the specified rectangular region. The crop region may be specified in one of two ways:<br>" +
        "(a) Via a bounding box, or<br>" +
        "(b) Via a {@code List<Point>} of length 2, containing the top-left and bottom-right crop locations.<br>" +
        "These may be specified statically (i.e., fixed crop region) via \"cropBox\" or \"cropPoints\" property, or dynamically " +
        "via \"cropName\" (which may specify a BoundingBox or List<Point> in the input Data instance).<br>" +
        "Furthermore, the bounding box and corner point coordinates may be specified in terms of either pixels or" +
        "\"fraction of image\" - specified via the \"coordsArePixels\" property. Note that if the crop region falls partly " +
        "outside the input image region, black padding will be added as necessary to keep the requested output size.")
public class ImageCropStep implements PipelineStep {

    @Schema(description = "Name of the Image or List<Image> field to crop")
    protected String imageName;

    @Schema(description = "Name of the input Data field used for dynamic cropping. May be a BoundingBox or List<Point>")
    protected String cropName;

    @Schema(description = "Static crop region defined as a List<Point>")
    protected List<Point> cropPoints;

    @Schema(description = "Static crop region defined as a BoundingBox")
    protected BoundingBox cropBox;

    @Schema(description = "Wether the crop region (BoundingBox / List<Point> are specified in pixels, or 'fraction of image'")
    protected boolean coordsArePixels = false;

    public ImageCropStep(@JsonProperty("inputNames") String imageName, @JsonProperty("cropName") String cropName,
                         @JsonProperty("cropPoints") List<Point> cropPoints, @JsonProperty("cropBox") BoundingBox cropBox,
                         @JsonProperty("coordsArePixels") boolean coordsArePixels) {
        this.imageName = imageName;
        this.cropName = cropName;
        this.cropPoints = cropPoints;
        this.cropBox = cropBox;
        this.coordsArePixels = coordsArePixels;
    }
}
