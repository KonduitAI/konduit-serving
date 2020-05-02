package ai.konduit.serving.pipeline.api;

import java.util.Map;
import java.util.Set;

public interface Configuration extends TextConfig {

    Set<String> keys();

    Map<String,Object> asMap();

    Object get(String key);

    Object getOrDefault(String key, Object defaultValue);


}
