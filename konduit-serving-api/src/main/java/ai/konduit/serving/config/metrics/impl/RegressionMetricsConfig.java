package ai.konduit.serving.config.metrics.impl;

import ai.konduit.serving.config.metrics.ColumnDistribution;
import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.util.ObjectMappers;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.*;
import lombok.experimental.Accessors;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The configuration associated with {@link ClassificationMetrics} -
 * this class contains metadata needed for exposing metrics correctly for
 * {@link ClassificationMetrics}
 *
 * @author Adam Gibson
 */
@Data
@Accessors(fluent=true)
@AllArgsConstructor
@NoArgsConstructor
public class RegressionMetricsConfig implements MetricsConfig {

    private List<String> regressionColumnLabels = new ArrayList<>(0);
    private List<SampleType> sampleTypes = new ArrayList<>(0);
    private List<ColumnDistribution> columnDistributions = new ArrayList<>(0);

    @Override
    @SneakyThrows
    public Class<? extends MeterBinder> metricsBinderImplementation() {
        return (Class<? extends MeterBinder>) Class.forName("ai.konduit.serving.metrics.RegressionMetrics");
    }

    @Override
    public Map<String, Object> configValues() {
        return Collections.singletonMap("regressionColumnLabels", regressionColumnLabels);
    }


    public  enum SampleType {
        SUM,
        MEAN,
        VARIANCE_POP,
        VARIANCE_NOPOP,
        MAX,
        MIN,
        STDDEV_POP,
        STDDEV_NOPOP,

    }

    public static RegressionMetricsConfig fromJson(String json) {
        return ObjectMappers.fromJson(json, RegressionMetricsConfig.class);
    }

    public static RegressionMetricsConfig fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, RegressionMetricsConfig.class);
    }

}
