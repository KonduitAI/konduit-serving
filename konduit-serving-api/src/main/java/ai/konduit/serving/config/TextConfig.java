package ai.konduit.serving.config;

import ai.konduit.serving.util.ObjectMappers;

public interface TextConfig {

    /**
     * Convert a configuration to a json string
     *
     * @return convert this object to a string
     */
    default String toJson(){
        return ObjectMappers.toJson(this);
    }

    /**
     * Convert a configuration to a yaml string
     *
     * @return the yaml representation of this configuration
     */
    default String toYaml(){
        return ObjectMappers.toYaml(this);
    }

}
