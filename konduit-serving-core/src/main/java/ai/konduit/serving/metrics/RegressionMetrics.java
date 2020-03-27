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
import ai.konduit.serving.config.metrics.impl.RegressionMetricsConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.datavec.api.transform.analysis.counter.StatCounter;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Regression metrics aggregated and displayed using
 * {@link StatCounter} and {@link Gauge}
 *
 * @author Adam Gibson
 */
public class RegressionMetrics implements MetricsRenderer {

    private Iterable<Tag> tags;
    @Getter
    private List<Gauge> outputStatsGauges;
    private List<StatCounter> statCounters;
    private Supplier<Double> statCounterSupplier;
    private RegressionMetricsConfig regressionMetricsConfig;

    public RegressionMetrics(RegressionMetricsConfig regressionMetricsConfig) {
        this(regressionMetricsConfig, Collections.emptyList());
    }

    public RegressionMetrics(RegressionMetricsConfig regressionMetricsConfig, Iterable<Tag> tags) {
        this.regressionMetricsConfig = regressionMetricsConfig;
        this.tags = tags;
        outputStatsGauges = new ArrayList<>();
        statCounters = new ArrayList<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for(int i = 0; i < regressionMetricsConfig.getRegressionColumnLabels().size(); i++) {
            StatCounter statCounter = new StatCounter();
            StatCounterSupplier statCounterSupplier = new StatCounterSupplier(statCounter,regressionMetricsConfig.getSampleTypes().get(i));
            outputStatsGauges.add(Gauge.builder(regressionMetricsConfig.getRegressionColumnLabels().get(i),statCounterSupplier)
                    .tags(tags)
                    .description("Regression values seen so far for label " + regressionMetricsConfig.getRegressionColumnLabels().get(i))
                    .baseUnit("regression.outcome")
                    .register(meterRegistry));


        }
    }



    private static class StatCounterSupplier implements Serializable,Supplier<Number> {
        private StatCounter statCounter;
        private RegressionMetricsConfig.SampleType sampleType;

        StatCounterSupplier(StatCounter statCounter, RegressionMetricsConfig.SampleType sampleType) {
            this.statCounter = statCounter;
            this.sampleType = sampleType;
        }

        @Override
        public Double get() {
            switch(sampleType) {
                case SUM:
                    return statCounter.getSum();
                case MEAN:
                    return statCounter.getMean();
                case MIN:
                    return statCounter.getMin();
                case MAX:
                    return statCounter.getMax();
                case STDDEV_POP:
                    return statCounter.getStddev(true);
                case STDDEV_NOPOP:
                    return statCounter.getStddev(false);
                case VARIANCE_POP:
                    return statCounter.getVariance(true);
                case VARIANCE_NOPOP:
                    return statCounter.getVariance(false);
                default:
                    return 0.0;
            }
        }
    }

    @Override
    public MetricsConfig config() {
        return regressionMetricsConfig;
    }

    @Override
    public void updateMetrics(Object... args) {
        if(args instanceof Record[]) {
            Record[] records = (Record[]) args[0];
            incrementClassificationCounters(records);
        }
        else if(args instanceof INDArray[]) {
            INDArray[] output = (INDArray[]) args[0];
            incrementClassificationCounters(output);

        }
    }


    private void incrementClassificationCounters(INDArray[] outputs) {
        synchronized (statCounters) {
            handleNdArray(outputs[0]);
        }

    }

    private void incrementClassificationCounters(Record[] records) {
        synchronized (statCounters) {
            NDArrayWritable ndArrayWritable = (NDArrayWritable) records[0].getRecord().get(0);
            handleNdArray(ndArrayWritable.get());
        }


    }

    private void handleNdArray(INDArray output) {
        if(output.isVector()) {
            for(int i = 0; i < output.length(); i++) {
                statCounters.get(i).add(output.getDouble(i));
            }
        }
        else if(output.isMatrix()) {
            for(int i = 0; i < output.rows(); i++) {
                for(int j = 0; j < output.columns(); j++) {
                    statCounters.get(i).add(output.getDouble(i,j));
                }
            }
        }
        else {
            throw new IllegalArgumentException("Only vectors and matrices supported right now");
        }
    }
}
