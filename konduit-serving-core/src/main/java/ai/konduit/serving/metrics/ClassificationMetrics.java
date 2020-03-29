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

import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.config.metrics.MetricsRenderer;
import ai.konduit.serving.config.metrics.impl.ClassificationMetricsConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classification metrics for counting number of classes
 * that occur during inference.
 *
 * @author Adam Gibson
 */
public class ClassificationMetrics implements MetricsRenderer {

    private Iterable<Tag> tags;
    @Getter
    private List<Counter> classCounterIncrement;

    private ClassificationMetricsConfig classificationMetricsConfig;

    public ClassificationMetrics(ClassificationMetricsConfig classificationMetricsConfig) {
        this(classificationMetricsConfig, Collections.emptyList());
    }

    public ClassificationMetrics(ClassificationMetricsConfig classificationMetricsConfig, Iterable<Tag> tags) {
        this.classificationMetricsConfig = classificationMetricsConfig;
        this.tags = tags;
        classCounterIncrement = new ArrayList<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for(int i = 0; i < classificationMetricsConfig.getClassificationLabels().size(); i++) {
            classCounterIncrement.add(Counter.builder(classificationMetricsConfig.getClassificationLabels().get(i))
                    .tags(tags)
                    .description("Classification counts seen so far for label " + classificationMetricsConfig.getClassificationLabels().get(i))
                    .baseUnit("classification.outcome")
                    .register(meterRegistry));


        }
    }

    @Override
    public MetricsConfig config() {
        return classificationMetricsConfig;
    }

    @Override
    public void updateMetrics(Object... args) {
        if(args[0] instanceof Record) {
            Record records = (Record) args[0];
            incrementClassificationCounters(new Record[]{records});
        }
        else if(args[0] instanceof Record[]) {
            Record[] records = (Record[]) args[0];
            incrementClassificationCounters(records);
        }
        else if(args[0] instanceof INDArray) {
            INDArray output = (INDArray) args[0];
            incrementClassificationCounters(new INDArray[] {output});
        }
        else if(args[0] instanceof INDArray[]) {
            INDArray[] output = (INDArray[]) args[0];
            incrementClassificationCounters(output);

        }
    }


    private void incrementClassificationCounters(INDArray[] outputs) {
        INDArray argMax = Nd4j.argMax(outputs[0], -1);
        for(int i = 0; i < argMax.length(); i++) {
            classCounterIncrement.get(argMax.getInt(i)).increment();
        }
    }

    private void incrementClassificationCounters(Record[] records) {
        if(classCounterIncrement != null) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) records[0].getRecord().get(0);
            INDArray output = ndArrayWritable.get();
            INDArray argMax = Nd4j.argMax(output, -1);
            for (int i = 0; i < argMax.length(); i++) {
                classCounterIncrement.get(argMax.getInt(i)).increment();
            }
        }
    }
}
