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

package ai.konduit.serving.data.image.step.bb.draw;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.data.image.convert.ImageToNDArrayConfig;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 *
 * Scale:
 * <ul>
 *     <li>NONE: No scaling</li>
 *     <li>AT_LEAST: Scale up if necessary, so H >= scaleH and W >= scaleW</li>
 *     <li>AT_MOST: Scale down if necessary, so H <= scaleH and W <= scaleW</li>
 * </ul>
 *
 */
@Builder
@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonName("DRAW_BOUNDING_BOX")
public class DrawBoundingBoxStep implements PipelineStep {
    public static final String DEFAULT_COLOR = "lime";

    public enum Scale {NONE, AT_LEAST, AT_MOST}

    private String imageName;       //If null: just find any image
    private String bboxName;       //If null: just find any BB's
    private boolean drawLabel;
    private boolean drawProbability;
    private Map<String,String> classColors;
    private String color;
    @Builder.Default
    private int lineThickness = 1;
    @Builder.Default
    private Scale scale = Scale.NONE;
    private int resizeH;
    private int resizeW;

    //Used to account for the fact that ImageToNDArray can crop images
    private ImageToNDArrayConfig imageToNDArrayConfig;
    private boolean drawCropRegion = false;
    private String cropRegionColor;

    /*
    Other things could add:
    - Upscale? (or minimum resolution, or always scale) - also aspect ratio part...
    - Text size
    - Text font
    - Line width
     */

    public DrawBoundingBoxStep(){
        //Normally this would be unnecessary to set default values here - but @Builder.Default values are NOT treated as normal default values.
        //Without setting defaults here again like this, the fields would actually be null
        this.scale = Scale.NONE;
        this.lineThickness = 1;
    }

}
