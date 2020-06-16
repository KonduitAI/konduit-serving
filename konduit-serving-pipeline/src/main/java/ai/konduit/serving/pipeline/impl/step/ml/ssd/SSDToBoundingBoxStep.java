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

package ai.konduit.serving.pipeline.impl.step.ml.ssd;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName("SSD_TO_BBOX")
@Schema(description = "A pipeline step that configures extraction of bounding boxes from an SSD model output.")
public class SSDToBoundingBoxStep implements PipelineStep {
    public static final String DEFAULT_OUTPUT_NAME = "bounding_boxes";
    // You can do new SSDToBoundingBoxStep().classLabels(SSDToBoundingBoxStep.COCO_LABELS)
    public static final String[] COCO_LABELS = new String[]{"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light", "fire hydrant", "street sign", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "hat", "backpack", "umbrella", "shoe", "eye glasses", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle", "plate", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed", "mirror", "dining table", "window", "desk", "toilet", "door", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "blender", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush", "hair brush"};

    //TODO config

    @Builder.Default
    @Schema(description = "A list of class labels.")
    protected String[] classLabels = null;

    @Builder.Default
    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.",
            defaultValue = "true")
    protected boolean keepOtherValues = true;

    @Builder.Default
    @Schema(description = "Threadshold to the output of the SSD models for fetching bounding boxes for.",
            defaultValue = "0.5")
    protected double threshold = 0.5;

    @Builder.Default
    @Schema(description = "Output key name where the bounding box will be contained in.",
            defaultValue = DEFAULT_OUTPUT_NAME)
    protected String outputName = DEFAULT_OUTPUT_NAME;


    public SSDToBoundingBoxStep(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.keepOtherValues = true;
        this.outputName = DEFAULT_OUTPUT_NAME;
        this.threshold = 0.5;
        this.classLabels = null;
    }

}
