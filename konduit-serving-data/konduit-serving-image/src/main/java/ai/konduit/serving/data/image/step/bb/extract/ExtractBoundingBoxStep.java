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

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

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
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class ExtractBoundingBoxStep implements PipelineStep {
    private String imageName;       //If null: just find any image
    private String bboxName;       //If null: just find any BB's
    private String outputName;
    @Builder.Default
    private boolean keepOtherFields = true;
    private Double aspectRatio = null;
    private Integer resizeH;
    private Integer resizeW;

    //Used to account for the fact that ImageToNDArray can crop images
    private ImageToNDArrayConfig imageToNDArrayConfig;

    public ExtractBoundingBoxStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the boolean default would be false
        keepOtherFields = true;
    }
}
