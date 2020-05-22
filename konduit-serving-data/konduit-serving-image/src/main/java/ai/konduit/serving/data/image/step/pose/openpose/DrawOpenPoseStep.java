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

package ai.konduit.serving.data.image.step.pose.openpose;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.step.grid.draw.DrawFixedGridStep;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Draw open pose points
 *
 * @author Alex Black
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
public class DrawOpenPoseStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

    private String imageName;               //If null: just find any image
    private String ndarrayName;             //NDArray. If null: find any (single) NDArray
    private ImageToNDArrayConfig i2nConfig;

}
