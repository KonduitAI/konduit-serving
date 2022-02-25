/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
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

package ai.konduit.serving.pipeline.impl.step.bbox.yolo;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.List;

/**
 * Convert an NDArray for the predictions of a YOLO model to {@code List<BoundingBox>}.<br>
 * The NDArray is assumed to be in "standard" YOLO output format, after activations (sigmoid/softmax) have been applied.<br>
 * Input must be a float/double NDArray with shape [minibatch, B*(5+C), H, W] (if nchw=true) or [minibatch, H, W, B*(5+C)] (if nchw=false)<br>
 * B = number of bounding box priors<br>
 * C = number of classes<br>
 * H = output/label height<br>
 * W = output/label width<br>
 * Along the channel dimension (for each box prior), we have the following values:
 * 0: px = predicted x location within grid cell, 0.0 to 1.0<br>
 * 1: py = predicted y location within grid cell, 0.0 to 1.0<br>
 * 2: pw = predicted width, in grid cell, for example 0.0 to H (for example, pw = 2.0 -> 2.0/W fraction of image)<br>
 * 3: ph = predicted height, in grid cell, for example 0.0 to H (for example, ph = 2.0 -> 2.0/H fraction of image)<br>
 * 4: c = object confidence - i.e., probability an object is present or not, 0.0 to 1.0<br>
 * 5 to 4+C = probability of class (given an object is present), 0.0 to 1.0, with values summing to 1.0<br>
 * <br>
 * Note that the height/width dimensions are grid cell units - for example, with 416x416 input, 32x downsampling by the network
 * we have 13x13 grid cells (each corresponding to 32 pixels in the input image). Thus, a centerX of 5.5 would be xPixels=5.5x32
 * = 176 pixels from left.<br>
 * Widths and heights are similar: in this example, a with of 13 would be the entire image (416 pixels), and a height of
 * 6.5 would be 6.5/13 = 0.5 of the image (208 pixels).
 *
 */
@Schema(description = "Convert an NDArray for the predictions of a YOLO model to {@code List<BoundingBox>}.<br>" +
        "The NDArray is assumed to be in \"standard\" YOLO output format, after activations (sigmoid/softmax) have been applied.<br>" +
        "Input must be a float/double NDArray with shape [minibatch, B*(5+C), H, W] (if nchw=true) or [minibatch, H, W, B*(5+C)] (if nchw=false)<br> " +
        "B = number of bounding box priors<br>" +
        "C = number of classes<br>" +
        "H = output/label height<br>" +
        "W = output/label width<br>" +
        "Along the channel dimension (for each box prior), we have the following values:" +
        "0: px = predicted x location within grid cell, 0.0 to 1.0<br>" +
        "1: py = predicted y location within grid cell, 0.0 to 1.0<br>" +
        "2: pw = predicted width, in grid cell, for example 0.0 to H (for example, pw = 2.0 -> 2.0/W fraction of image)<br>" +
        "3: ph = predicted height, in grid cell, for example 0.0 to H (for example, ph = 2.0 -> 2.0/H fraction of image)<br>" +
        "4: c = object confidence - i.e., probability an object is present or not, 0.0 to 1.0<br>" +
        "5 to 4+C = probability of class (given an object is present), 0.0 to 1.0, with values summing to 1.0<br>" +
        "<br>" +
        "Note that the height/width dimensions are grid cell units - for example, with 416x416 input, 32x downsampling by the network" +
        "we have 13x13 grid cells (each corresponding to 32 pixels in the input image). Thus, a centerX of 5.5 would be xPixels=5.5x32" +
        "= 176 pixels from left.<br>" +
        "Widths and heights are similar: in this example, a with of 13 would be the entire image (416 pixels), and a height of" +
        "6.5 would be 6.5/13 = 0.5 of the image (208 pixels).")
@Data
@Accessors(fluent = true)
@JsonName("YOLO_BBOX")
public class YoloToBoundingBoxStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "bounding_boxes";

    @Schema(description = "Name of the input - optional. If not set, the input is inferred (assuming a single NDArray exists in the input)")
    protected String input;

    @Schema(description = "Name of the input - optional. If not set, \"bounding_boxes\" is used")
    protected String output;

    @Schema(description = "The data format - NCHW (true) or NHWC (false) aka 'channels first' (true) or 'channels last' (false)")
    protected boolean nchw = true;

    @Schema(description = "The threshold, in range 0.0 to 1.0. Any boxes with object confidence less than this will be ignored")
    protected double threshold = 0.5;

    @Schema(description = "Non-max suppression threshold to use, to filter closely overlapping objects")
    protected double nmsThreshold = 0.5;

    @Schema(description = "Number of classes. Not required if classLabels are provided")
    protected Integer numClasses;

    @Schema(description = "Optional - names of the object classes")
    protected List<String> classLabels;

    @Schema(description = "If true: keep all other input fields in the Data instance. False: only return the List<BoundingBox>")
    protected boolean keepOtherValues = true;


    @Tolerate
    public YoloToBoundingBoxStep classLabels(String... labels){
        this.classLabels = Arrays.asList(labels);
        return this;
    }

}
