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

package ai.konduit.serving.pipeline.steps;

import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.ModelStep;
import ai.konduit.serving.pipeline.step.PythonStep;
import ai.konduit.serving.pipeline.step.TransformProcessStep;
import org.junit.Test;

public class StepConfigClassTester {

    @Test
    public void testPipelineClasses() throws Exception {
        Class.forName(new ImageLoadingStep().pipelineStepClazz());
        Class.forName(new ModelStep().pipelineStepClazz());
        Class.forName(new PythonStep().pipelineStepClazz());
        Class.forName(new TransformProcessStep().pipelineStepClazz());

    }

}
