package ai.konduit.serving.deeplearning4j;

import ai.konduit.serving.pipeline.api.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DL4JConfiguration implements Configuration {


    @Override
    public Set<String> keys() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.emptyMap();
    }

    @Override
    public Object get(String key) {
        throw new IllegalStateException("No key \"" + key + "\" exists");
    }

    @Override
    public Object getOrDefault(String key, Object defaultValue) {
        return defaultValue;
    }
}
