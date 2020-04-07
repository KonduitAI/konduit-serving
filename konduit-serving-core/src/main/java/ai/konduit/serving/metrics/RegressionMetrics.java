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

import ai.konduit.serving.config.metrics.ColumnDistribution;
import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.config.metrics.MetricsRenderer;
import ai.konduit.serving.config.metrics.impl.RegressionMetricsConfig;
import ai.konduit.serving.util.MetricRenderUtils;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import org.datavec.api.records.Record;
import org.datavec.api.transform.analysis.counter.StatCounter;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private RegressionMetricsConfig regressionMetricsConfig;

    public RegressionMetrics(RegressionMetricsConfig regressionMetricsConfig) {
        this(regressionMetricsConfig, Arrays.asList(new ImmutableTag("machinelearning","regression")));
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
            statCounters.add(statCounter);
            ColumnDistribution columnDistribution = regressionMetricsConfig.getColumnDistributions() != null &&
                    regressionMetricsConfig.getColumnDistributions().size() == regressionMetricsConfig.getRegressionColumnLabels().size() ?
                    regressionMetricsConfig.getColumnDistributions().get(i) : null;
            StatCounterSupplier statCounterSupplier = new StatCounterSupplier(statCounter,regressionMetricsConfig.getSampleTypes().get(i),columnDistribution);
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
        private ColumnDistribution columnDistribution;
        StatCounterSupplier(StatCounter statCounter, RegressionMetricsConfig.SampleType sampleType,ColumnDistribution columnDistribution) {
            this.statCounter = statCounter;
            this.sampleType = sampleType;
            this.columnDistribution = columnDistribution;
        }

        @Override
        public Double get() {
            Double ret = null;
            switch(sampleType) {
                case SUM:
                    ret = statCounter.getSum();
                    break;
                case MEAN:
                    ret = statCounter.getMean();
                    break;
                case MIN:
                    ret = statCounter.getMin();
                    break;
                case MAX:
                    ret = statCounter.getMax();
                    break;
                case STDDEV_POP:
                    ret = statCounter.getStddev(true);
                    break;
                case STDDEV_NOPOP:
                    ret = statCounter.getStddev(false);
                    break;
                case VARIANCE_POP:
                    ret = statCounter.getVariance(true);
                    break;
                case VARIANCE_NOPOP:
                    ret = statCounter.getVariance(false);
                    break;
                default:
                    return 0.0;
            }

            if(columnDistribution != null) {
                ret = MetricRenderUtils.deNormalizeValue(ret,columnDistribution);
            }

            return ret;
        }
    }

    @Override
    public MetricsConfig config() {
        return regressionMetricsConfig;
    }

    @Override
    public void updateMetrics(Object... args) {
        if(args[0] instanceof Record) {
            Record records = (Record) args[0];
            incrementRegressionCounters(new Record[]{records});
        }
        else if(args[0] instanceof Record[]) {
            Record[] records = (Record[]) args[0];
            incrementRegressionCounters(records);
        }
        else if(args[0] instanceof INDArray) {
            INDArray output = (INDArray) args[0];
            incrementRegressionCounters(new INDArray[] {output});
        }
        else if(args[0] instanceof INDArray[]) {
            INDArray[] output = (INDArray[]) args[0];
            incrementRegressionCounters(output);

        }
    }


    private void incrementRegressionCounters(INDArray[] outputs) {
        synchronized (statCounters) {
            handleNdArray(outputs[0]);
        }

    }

    private void incrementRegressionCounters(Record[] records) {
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
        else if(output.isMatrix() && output.length() > 1) {
            for(int i = 0; i < output.rows(); i++) {
                for(int j = 0; j < output.columns(); j++) {
                    statCounters.get(i).add(output.getDouble(i,j));
                }
            }
        }
        else if(output.isScalar()) {
            statCounters.get(0).add(output.sumNumber().doubleValue());
        }
        else {
            throw new IllegalArgumentException("Only vectors and matrices supported right now");
        }
    }
}
