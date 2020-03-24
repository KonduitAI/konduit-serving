package ai.konduit.serving.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classification metrics for counting number of classes
 * that occur during inference.
 *
 * @author Adam Gibson
 */
public class ClassificationMetrics implements MeterBinder {

    private List<String> labels;
    private Iterable<Tag> tags;
    @Getter
    private List<Counter> classCounters;

    public ClassificationMetrics(List<String> labels) {
        this(labels, Collections.emptyList());
    }

    public ClassificationMetrics(List<String> labels, Iterable<Tag> tags) {
        this.labels = labels;
        this.tags = tags;
        classCounters = new ArrayList<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for(int i = 0; i < labels.size(); i++) {
            classCounters.add(Counter.builder(labels.get(i))
                    .tags(tags)
                    .baseUnit("classification.outcome")
                    .register(meterRegistry));


        }
    }
}
