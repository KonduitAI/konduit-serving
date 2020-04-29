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

package ai.konduit.serving.pipeline;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PipelineSerializationTests {

    @Test
    public void testFromYaml() throws Exception {
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .step(new PythonStep())
                .step(new PythonStep())
                .servingConfig(ServingConfig.builder().build())
                .build();
        assertEquals(inferenceConfiguration, InferenceConfiguration.fromYaml(inferenceConfiguration.toYaml()));
    }

    @Test
    public void testPipelineSerialization() throws Exception {
        ImageLoadingStep imageLoadingStepConfig = ImageLoadingStep.builder()
                .dimensionsConfig("default", new Long[]{426L, 426L, 3L})
                .dimensionsConfig("1", new Long[]{426L, 426L, 3L})
                .build();

        InferenceConfiguration config = InferenceConfiguration.builder()
                .step(imageLoadingStepConfig)
                .step(imageLoadingStepConfig)
                .build();
        System.out.println(config.toJson());
    }
}
