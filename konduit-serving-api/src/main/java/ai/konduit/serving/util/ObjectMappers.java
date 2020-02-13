/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.util;

import lombok.NonNull;
import org.nd4j.shade.jackson.annotation.JsonAutoDetect;
import org.nd4j.shade.jackson.annotation.JsonInclude;
import org.nd4j.shade.jackson.annotation.PropertyAccessor;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.DeserializationFeature;
import org.nd4j.shade.jackson.databind.MapperFeature;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.nd4j.shade.jackson.databind.SerializationFeature;
import org.nd4j.shade.jackson.dataformat.yaml.YAMLFactory;
import org.nd4j.shade.jackson.datatype.joda.JodaModule;

import java.io.IOException;

/**
 * A simple object mapper holder for using one single {@link ObjectMapper} across the whole project.
 */
public class ObjectMappers {

    private static final ObjectMapper jsonMapper = configureMapper(new ObjectMapper());
    private static final ObjectMapper yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()));

    private ObjectMappers() {
    }


    /**
     * Get a single object mapper for use with reading and writing JSON
     *
     * @return JSON object mapper
     */
    public static ObjectMapper json() {
        return jsonMapper;
    }

    /**
     * Get a single object mapper for use with reading and writing YAML
     *
     * @return YAML object mapper
     */
    public static ObjectMapper yaml(){
        return yamlMapper;
    }

    private static ObjectMapper configureMapper(ObjectMapper ret) {
        ret.registerModule(new JodaModule());
        ret.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ret.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        ret.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        ret.enable(SerializationFeature.INDENT_OUTPUT);
        ret.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        ret.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        ret.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        ret.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return ret;
    }


    public static String toYaml(Object o){
        try{
            return yaml().writeValueAsString(o);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Error converting object of class " + o.getClass().getName() + " to YAML", e);
        }
    }

    public static String toJson(Object o){
        try{
            return json().writeValueAsString(o);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Error converting object of class " + o.getClass().getName() + " to JSON", e);
        }
    }

    public static <T> T fromYaml(@NonNull String yaml, @NonNull Class<T> c){
        try {
            return yaml().readValue(yaml, c);
        } catch (IOException e){
            throw new RuntimeException("Error deserializing YAML string to class " + c.getName(), e);
        }
    }

    public static <T> T fromJson(@NonNull String json, @NonNull Class<T> c){
        try {
            return json().readValue(json, c);
        } catch (IOException e){
            throw new RuntimeException("Error deserializing JSON string to class " + c.getName(), e);
        }
    }
}
