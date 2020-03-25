package ai.konduit.serving.config.metrics.impl;

import ai.konduit.serving.config.metrics.MetricsConfig;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.*;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationMetricsConfig implements MetricsConfig {

    @Builder.Default
    private List<String> classificationLabels = new ArrayList<>(0);

    @Override
    @SneakyThrows
    public Class<? extends MeterBinder> metricsBinderImplementation() {
        return (Class<? extends MeterBinder>) Class.forName("ai.konduit.serving.metrics.ClassificationMetrics");
    }

    @Override
    public Map<String, Object> configValues() {
        return Collections.singletonMap("classificationLabels",classificationLabels);
    }
}
