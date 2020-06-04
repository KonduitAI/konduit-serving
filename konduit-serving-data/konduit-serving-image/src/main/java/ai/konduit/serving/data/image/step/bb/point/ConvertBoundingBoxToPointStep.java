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

package ai.konduit.serving.data.image.step.bb.point;

import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * ConvertBoundingBoxToPointStep: Given one or more bounding boxes, create a point representation of them.<br>
 * You can choose from the following methods of converting the bounding box to a point: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT,
 * BOTTOM_RIGHT, CENTER<br>
 * Note: supports both {@code BoundingBox} and {@code List<BoundingBox>} fields. If the input is as single value,
 * the output will be a single value; if the input is a list, the output will be a list.<br>
 * <br>
 * Note: The output is a bounding box again, with it's top left point being set to the actual point and a zero width and
 * height.
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class ConvertBoundingBoxToPointStep implements PipelineStep {
    public enum ConversionMethod {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER}

    private String bboxName;       //If null: just find any BB's
    private String outputName;
    @Builder.Default
    private boolean keepOtherFields = true;
    @Builder.Default
    private ConversionMethod method = ConversionMethod.BOTTOM_LEFT;


    public ConvertBoundingBoxToPointStep() {
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the boolean default would be false, the enum would be null
        keepOtherFields = true;

        method = ConversionMethod.BOTTOM_LEFT;
    }
}
