package ai.konduit.serving.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.nd4j.linalg.primitives.Pair;

/**
 * Utility class for dealing with {@link io.vertx.micrometer.impl.MicrometerMetrics}
 *
 * @author Adam Gibson
 */
public class MetricsUtils {

    /**
     * Sets up promethues and returns the
     * registry
     * @return
     */
    public static Pair<MicrometerMetricsOptions,MeterRegistry> setupPrometheus() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(registry)
                .setPrometheusOptions(new VertxPrometheusOptions()
                        .setEnabled(true));
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        return Pair.of(micrometerMetricsOptions,registry);

    }

}
