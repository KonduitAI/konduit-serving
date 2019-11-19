/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.pipeline.step;

import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.datavec.image.transform.ImageTransformProcess;

import java.io.Serializable;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
public class ImageLoadingStep extends PipelineStep implements Serializable {

    private int originalImageHeight;
    private int originalImageWidth;

    private boolean updateOrderingBeforeTransform = false;

    @Singular
    private Map<String, Long[]> dimensionsConfigs;

    private String imageProcessingRequiredLayout = "NCHW", imageProcessingInitialLayout = "NCHW";

    @Singular
    private Map<String, ImageTransformProcess> imageTransformProcesses;

    private ObjectDetectionConfig objectDetectionConfig;

    public boolean initialImageLayoutMatchesFinal() {
        if(getImageProcessingInitialLayout() != null && getImageProcessingRequiredLayout() != null)
            return getImageProcessingInitialLayout().equals(getImageProcessingRequiredLayout());
        return true;
    }

    @Override
    public String pipelineStepClazz() {
        return "ai.konduit.serving.pipeline.steps.ImageTransformProcessStepRunner";
    }
}