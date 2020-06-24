package ai.konduit.serving.metrics;

import ai.konduit.serving.config.metrics.ColumnDistribution;
import ai.konduit.serving.config.metrics.impl.RegressionMetricsConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerType;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class RegressionMetricsTest {

    @Test
    public void testRegressionValue() {
        RegressionMetricsConfig regressionMetricsConfig = RegressionMetricsConfig.builder()
                .regressionColumnLabels(Arrays.asList("test"))
                .sampleTypes(Arrays.asList(RegressionMetricsConfig.SampleType.MEAN))
                .columnDistributions(Arrays.asList(ColumnDistribution.builder()
                .max(1.0).min(0.0).normalizerType(NormalizerType.MIN_MAX).build()))
                .build();

        RegressionMetrics regressionMetrics = new RegressionMetrics(regressionMetricsConfig);
        regressionMetrics.bindTo(new SimpleMeterRegistry());

        regressionMetrics.updateMetrics(new INDArray[] {Nd4j.scalar(1.0).reshape(1,1)});

        Gauge gauge = regressionMetrics.getOutputStatsGauges().get(0);
        double value = gauge.value();
        assertEquals(1.0,value,1e-1);
    }

}
