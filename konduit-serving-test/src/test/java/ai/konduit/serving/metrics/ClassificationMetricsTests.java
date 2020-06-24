/*
 *
 *  * ******************************************************************************
 *  *
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
package ai.konduit.serving.metrics;

import ai.konduit.serving.config.metrics.impl.ClassificationMetricsConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ClassificationMetricsTests {

    @Test
    public void testClassificationMetricsReset() {
        ClassificationMetricsConfig classificationMetricsConfig = ClassificationMetricsConfig.builder()
                .classificationLabels(Arrays.asList("0"))
                .build();

        ClassificationMetrics classificationMetrics = new ClassificationMetrics(classificationMetricsConfig);
        classificationMetrics.bindTo(new SimpleMeterRegistry());
        INDArray arr = Nd4j.scalar(1.0).reshape(1,1);
        classificationMetrics.updateMetrics(new INDArray[] {arr});
        double value = classificationMetrics.getClassCounterIncrement().get(0).value();
        assertEquals(1.0,value,1e-3);
        assertEquals(0.0,classificationMetrics.getClassCounterIncrement().get(0).value(),1e-3);
    }

}
