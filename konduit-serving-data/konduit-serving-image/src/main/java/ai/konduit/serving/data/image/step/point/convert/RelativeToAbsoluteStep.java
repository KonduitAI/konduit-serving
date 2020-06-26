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

package ai.konduit.serving.data.image.step.point.convert;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.List;

/**
 * For a given set of {@code Point}, {@code List<Point>}, {@code BoundingBox} or {@code List<BoundingBox>} that are
 * defined in relative terms (i.e., all values 0.0 to 1.0 in terms of "fraction of image height/width"), convert these
 * to absolute values (i.e., pixels) using:<br>
 * (a) An input image, as specified via the imageName configuration, OR<br>
 * (b) An image height, as specified by imageH and imageW configuration<br>
 * <br>
 * Note that an ImageToNDArrayConfig can be provided, to account for the fact that the original image may have been
 * cropped before being passed into a network that produced the Points or BoundingBoxes.<br>
 * If the imageToNDArrayConfig field is null, it is assumed no cropping has occurred.
 *
 * @author Alex Black
 */
@Schema(description = "For a given set of {@code Point}, {@code List<Point>}, {@code BoundingBox} or {@code List<BoundingBox>} that are " +
        "defined in relative terms (i.e., all values 0.0 to 1.0 in terms of \"fraction of image height/width\"), convert these " +
        "to absolute values (i.e., pixels) using:<br>" +
        "(a) An input image, as specified via the imageName configuration, OR<br>" +
        "(b) An image height, as specified by imageH and imageW configuration<br>" +
        "<br>" +
        "Note that an ImageToNDArrayConfig can be provided, to account for the fact that the original image may have been " +
        "cropped before being passed into a network that produced the Points or BoundingBoxes.<br>" +
        "If the imageToNDArrayConfig field is null, it is assumed no cropping has occurred.")
@Data
@Accessors(fluent = true)
@JsonName("RELATIVE_TO_ABSOLUTE")
public class RelativeToAbsoluteStep implements PipelineStep {

    @Schema(description = "Optional - the name of the field in the input Data containing the Image to use (to determine H/W)")
    protected String imageName;
    @Schema(description = "If imageName is not specified - height of the input image to use")
    protected Integer imageH;
    @Schema(description = "If imageName is not specified - width of the input image to use")
    protected Integer imageW;
    @Schema(description = "Optional - used to account for the fact that the image may have been cropped before being passed" +
            " into a network that produced the points/bounding box. This allows for them to be offset, so the boxes/coordinates" +
            " are specified in terms of the original image, not the cropped image")
    protected ImageToNDArrayConfig imageToNDArrayConfig;
    @Schema(description = "Optional - the name of the Point, List<Point>, BoundingBox or List<BoundingBox> fields to convert." +
            " If not set, the step will convert any/all fields of those types")
    protected List<String> toConvert;

    @Tolerate
    public RelativeToAbsoluteStep toConvert(String... toConvert){
        return toConvert(Arrays.asList(toConvert));
    }
}
