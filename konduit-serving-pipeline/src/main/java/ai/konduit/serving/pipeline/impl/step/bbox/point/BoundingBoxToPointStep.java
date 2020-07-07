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

package ai.konduit.serving.pipeline.impl.step.bbox.point;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * BoundingBoxToPointStep: Given one or more bounding boxes, create a point representation of them.<br>
 * You can choose from the following methods of converting the bounding box to a point: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT,
 * BOTTOM_RIGHT, CENTER.<br>
 * Set to CENTER by default.<br>
 * Note: supports both {@code BoundingBox} and {@code List<BoundingBox>} fields. If the input is as single value,
 * the output will be a single value; if the input is a list, the output will be a list.<br>
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonName("BOUNDING_BOX_TO_POINT")
public class BoundingBoxToPointStep implements PipelineStep {
    public enum ConversionMethod {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER}

    @Schema(description = "Name of the bounding boxes key from the previous step. If set to null, it will try to find any bounding box in the incoming data instance.")
    private String bboxName;

    @Schema(description = "Name of the key of point representation from this step")
    private String outputName;

    @Schema(description = "If true, other data key and values from the previous step are kept and passed on to the next step as well.",
            defaultValue = "true")
    private boolean keepOtherFields = true;

    @Schema(description =  "You can choose from the following methods of converting the bounding box to a point: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT,\n" +
            "BOTTOM_RIGHT, CENTER. Set to CENTER by default")
    private ConversionMethod method = ConversionMethod.CENTER;
}
