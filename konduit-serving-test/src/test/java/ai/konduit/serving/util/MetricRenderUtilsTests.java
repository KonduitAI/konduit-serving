package ai.konduit.serving.util;

import ai.konduit.serving.config.metrics.ColumnDistribution;
import org.junit.Test;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerType;

import static org.junit.Assert.assertEquals;

public class MetricRenderUtilsTests {

    @Test
    public void testMinMax() {
        ColumnDistribution columnDistribution = ColumnDistribution.builder()
                .max(1.0)
                .min(0.0)
                .normalizerType(NormalizerType.MIN_MAX)
                .build();

        double value = 1.4;
        assertEquals(value,MetricRenderUtils.deNormalizeValue(MetricRenderUtils.normalizeValue(value,columnDistribution),columnDistribution),1e-1);



    }

    @Test
    public void testStandardize() {
        ColumnDistribution columnDistribution = ColumnDistribution.builder()
                .standardDeviation(1.0)
                .mean(0.0)
                .normalizerType(NormalizerType.STANDARDIZE)
                .build();

        double value = 1.4;
        assertEquals(value,MetricRenderUtils.deNormalizeValue(MetricRenderUtils.normalizeValue(value,columnDistribution),columnDistribution),1e-1);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() {
        ColumnDistribution columnDistribution = ColumnDistribution.builder()
                .standardDeviation(1.0)
                .mean(0.0)
                .normalizerType(NormalizerType.IMAGE_MIN_MAX)
                .build();

        double value = 1.4;
        assertEquals(value,MetricRenderUtils.deNormalizeValue(MetricRenderUtils.normalizeValue(value,columnDistribution),columnDistribution),1e-1);

    }

}
