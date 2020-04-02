package ai.konduit.serving.config.metrics.impl;

import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.config.metrics.MetricsRenderer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * An {@link MeterBinder} wrapper that provides default no op
 * {@link MetricsRenderer} implementations.
 *
 * @author Adam Gibson
 */
@AllArgsConstructor
@NoArgsConstructor
public class MetricsBinderRendererAdapter implements MetricsRenderer {

    private MeterBinder meterBinder;

    @Override
    public MetricsConfig config() {
        return null;
    }

    @Override
    public void updateMetrics(Object... args) {

    }

    @Override
    public void bindTo(MeterRegistry registry) {
        meterBinder.bindTo(registry);
    }
}
