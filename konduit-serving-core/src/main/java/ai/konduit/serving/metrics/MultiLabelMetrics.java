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
package ai.konduit.serving.metrics;

import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.config.metrics.MetricsRenderer;
import ai.konduit.serving.config.metrics.impl.MultiLabelMetricsConfig;
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
 * A {@link MetricsRenderer} that takes in matrices of counts
 * where each column (indexed by what is specified in the {@link #multiLabelMetricsConfig}
 * specified in {@link #updateMetrics(Object...)}
 * is a count of how to increment the column.
 *
 * Note that similar to {@link ClassificationMetrics}
 * the counts will reset upon sampling via prometheus.
 *
 * @author Adam Gibson
 */
public class MultiLabelMetrics implements MetricsRenderer {
    @Getter
    private MultiLabelMetricsConfig multiLabelMetricsConfig;
    @Getter
    private List<CurrentClassTrackerCount> classTrackerCounts;

    private Iterable<Tag> tags;
    @Getter
    private List<Gauge> classCounterIncrement;

    public MultiLabelMetrics(MultiLabelMetricsConfig multiLabelMetricsConfig, Iterable<Tag> tags) {
        this.multiLabelMetricsConfig = multiLabelMetricsConfig;
        this.tags = tags;
        classTrackerCounts = new ArrayList<>();
        classCounterIncrement = new ArrayList<>();
    }

    public MultiLabelMetrics(MultiLabelMetricsConfig multiLabelMetricsConfig) {
        this(multiLabelMetricsConfig, Arrays.asList(new ImmutableTag("machinelearning","multilabel")));
    }
    @Override
    public MetricsConfig config() {
        return multiLabelMetricsConfig;
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
            CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(argMax.getInt(i));
            classTrackerCount.increment(1.0);
        }
    }

    private void incrementClassificationCounters(Record[] records) {
        if(classCounterIncrement != null) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) records[0].getRecord().get(0);
            INDArray output = ndArrayWritable.get();
            INDArray argMax = Nd4j.argMax(output, -1);
            for (int i = 0; i < argMax.length(); i++) {
                CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(argMax.getInt(i));
                classTrackerCount.increment(1.0);
            }
        }
    }

    private void handleNdArray(INDArray array) {

        if(array.isScalar()) {
            CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(0);
            classTrackerCount.increment(array.getDouble(0));

        }
        else if(array.isMatrix()) {
            for(int i = 0; i < array.rows(); i++) {
                for(int j = 0; j < array.columns(); j++) {
                    CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(array.getInt(i));
                    classTrackerCount.increment(array.getDouble(i,j));

                }
            }
        }
        else if(array.isVector()) {
            for (int i = 0; i < array.length(); i++) {
                CurrentClassTrackerCount classTrackerCount = classTrackerCounts.get(array.getInt(i));
                classTrackerCount.increment(array.getDouble(i));
            }
        }

    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for(int i = 0; i < multiLabelMetricsConfig.getLabels().size(); i++) {
            CurrentClassTrackerCount classTrackerCount = new CurrentClassTrackerCount();
            classTrackerCounts.add(classTrackerCount);
            classCounterIncrement.add(Gauge.builder(multiLabelMetricsConfig.getLabels().get(i),classTrackerCount)
                    .tags(tags)
                    .description("Multi-label Classification counts seen so far for label " + multiLabelMetricsConfig.getLabels().get(i))
                    .baseUnit("multilabelclassification.outcome")
                    .register(registry));


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
}
