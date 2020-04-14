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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.AtomicDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Classification metrics for counting number of classes
 * that occur during inference.
 *
 * @author Adam Gibson
 */
public class ClassificationMetrics implements MetricsRenderer {

    private Iterable<Tag> tags;
    @Getter
    private List<Gauge> classCounterIncrement;
    @Getter
    private List<CurrentClassTrackerCount> classTrackerCounts;
    private ClassificationMetricsConfig classificationMetricsConfig;

    public ClassificationMetrics(ClassificationMetricsConfig classificationMetricsConfig) {
        this(classificationMetricsConfig, Arrays.asList(new ImmutableTag("machinelearning","classification")));
    }

    public ClassificationMetrics(ClassificationMetricsConfig classificationMetricsConfig, Iterable<Tag> tags) {
        this.classificationMetricsConfig = classificationMetricsConfig;
        this.tags = tags;
        classCounterIncrement = new ArrayList<>();
        classTrackerCounts = new ArrayList<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for(int i = 0; i < classificationMetricsConfig.getClassificationLabels().size(); i++) {
            CurrentClassTrackerCount classTrackerCount = new CurrentClassTrackerCount();
            classTrackerCounts.add(classTrackerCount);
            classCounterIncrement.add(Gauge.builder(classificationMetricsConfig.getClassificationLabels().get(i),classTrackerCount)
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


    /**
     * A counter that resets the in memory value when
     * the metric is exported. It is assumed that when exported,
     * a storage system captures the sampled value.
     *
     */
    private static class CurrentClassTrackerCount implements Supplier<Number> {

        private AtomicDouble currCounter = new AtomicDouble(0);

        public void increment(double numberToIncrementBy) {
            currCounter.getAndAdd(numberToIncrementBy);
        }

        public void reset() {
            currCounter.set(0.0);
        }

        @Override
        public Number get() { ;
            double ret = currCounter.get();
            reset();
            return ret;
        }
    }


    private void incrementClassificationCounters(INDArray[] outputs) {
        handleNdArray(outputs[0]);
    }

    private void incrementClassificationCounters(Record[] records) {
        if(classCounterIncrement != null) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) records[0].getRecord().get(0);
            INDArray output = ndArrayWritable.get();
            handleNdArray(output);
        }
    }

    private void handleNdArray(INDArray array) {
        INDArray argMax = Nd4j.argMax(array, -1);
        for(int i = 0; i < argMax.length(); i++) {
            CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(argMax.getInt(i));
            classTrackerCount.increment(1.0);
        }
    }
}
