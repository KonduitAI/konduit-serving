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

package ai.konduit.serving.data.image.step.facial;

import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.data.image.step.bb.draw.DrawBoundingBoxStep;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;


@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class DrawFacialKeyPointsStep implements PipelineStep {

    public static final String DEFAULT_OUTPUT_NAME = "image";
    public enum Scale {NONE, AT_LEAST, AT_MOST}

    private ImageToNDArrayConfig imageToNDArrayConfig;
    private int resizeH;
    private int resizeW;
    private boolean drawCropRegion = false;


    @Builder.Default
    private Scale scale = Scale.NONE;

    private String landmarkArray;
    private String image;
    private String outputName;

    public DrawFacialKeyPointsStep(){
        this.scale = Scale.NONE;

    }


    }


