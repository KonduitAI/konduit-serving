/*
 *       Copyright (c) 2019 Konduit AI.
 *
 *       This program and the accompanying materials are made available under the
 *       terms of the Apache License, Version 2.0 which is available at
 *       https://www.apache.org/licenses/LICENSE-2.0.
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *       WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *       License for the specific language governing permissions and limitations
 *       under the License.
 *
 *       SPDX-License-Identifier: Apache-2.0
 *
 */

package ai.konduit.serving.config;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigSerdeTests {

    @Test
    public void testImageLoadingSerde() throws Exception {
        ObjectDetectionConfig objectDetectionConfig = ObjectDetectionConfig.builder().build();
        ImageLoadingStep imageLoadingStepConfig = ImageLoadingStep.builder()
                .inputNames(Collections.singletonList("image_tensor"))
                .outputNames(Collections.singletonList("detection_classes"))
                .objectDetectionConfig(objectDetectionConfig)
                .build();

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .step(imageLoadingStepConfig)
                .build();
        assertNotNull(inferenceConfiguration.toJson());
        assertEquals(inferenceConfiguration,InferenceConfiguration.fromJson(inferenceConfiguration.toJson()));
    }

}
