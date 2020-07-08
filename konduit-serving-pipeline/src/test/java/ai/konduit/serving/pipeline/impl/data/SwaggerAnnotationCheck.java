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
package ai.konduit.serving.pipeline.impl.data;

import ai.konduit.serving.common.test.BaseSwaggerAnnotationCheck;
import ai.konduit.serving.pipeline.impl.pipeline.PipelineProfilerTest;
import ai.konduit.serving.pipeline.impl.testpipelines.count.CountStep;
import ai.konduit.serving.pipeline.impl.testpipelines.fn.FunctionStep;
import ai.konduit.serving.pipeline.impl.util.CallbackStep;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SwaggerAnnotationCheck extends BaseSwaggerAnnotationCheck {
    @Override
    public String getPackageName() {
        return "ai.konduit.serving.pipeline";
    }

    @Override
    public Set<Class<?>> ignores() {
        Set<Class<?>> set = new HashSet<>();
        set.add(PipelineProfilerTest.TestStep.class);
        set.add(CallbackStep.class);
        set.add(ai.konduit.serving.pipeline.impl.testpipelines.callback.CallbackStep.class);
        set.add(FunctionStep.class);
        set.add(CountStep.class);
        return set;
    }


    @Test
    public void checkAnnotations() throws ClassNotFoundException {
        runTest();
    }
}
