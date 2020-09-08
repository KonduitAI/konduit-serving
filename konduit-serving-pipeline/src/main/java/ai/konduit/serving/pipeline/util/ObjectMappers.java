/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package ai.konduit.serving.pipeline.util;

import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.shade.jackson.annotation.JsonAutoDetect;
import org.nd4j.shade.jackson.annotation.JsonInclude;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.PropertyAccessor;
import org.nd4j.shade.jackson.core.JsonProcessingException;
import org.nd4j.shade.jackson.databind.*;
import org.nd4j.shade.jackson.databind.jsontype.NamedType;
import org.nd4j.shade.jackson.dataformat.yaml.YAMLFactory;
import org.nd4j.shade.jackson.dataformat.yaml.YAMLGenerator;
import org.nd4j.shade.jackson.datatype.joda.JodaModule;

import java.io.IOException;
import java.util.*;

/**
 * A simple object mapper holder for using one single {@link ObjectMapper} across the whole project.
 */
@Slf4j
public class ObjectMappers {

    private static final Set<JsonSubType> manuallyRegisteredSubtypes = new HashSet<>();

    private static ObjectMapper jsonMapper = configureMapper(new ObjectMapper());
    private static ObjectMapper yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)  // For preventing YAML from adding `!<TYPE>` with polymorphic objects
            // and use Jackson's type information mechanism.
    ));

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
    public static ObjectMapper yaml() {
        return yamlMapper;
    }

    private static ObjectMapper configureMapper(ObjectMapper ret) {
        ret.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ret.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        ret.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);         //Use order in which fields are defined in classes
        ret.enable(SerializationFeature.INDENT_OUTPUT);
        ret.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        ret.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        ret.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        ret.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (ret.getFactory() instanceof YAMLFactory) {
            ret.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        }

        ret.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);

        //Configure subtypes - via service loader from other modules
        List<JsonSubType> l = getAllSubtypes();
        for(JsonSubType t : l){
            NamedType nt = new NamedType(t.getSubtype(), t.getName());
            ret.registerSubtypes(nt);
        }

        return ret;
    }

    /**
     * Convert the specified object to a YAML String, throwing an unchecked exception (RuntimeException) if conversion fails
     *
     * @param o Object
     * @return Object as YAML
     */
    public static String toYaml(@NonNull Object o) {
        try {
            return yaml().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object of class " + o.getClass().getName() + " to YAML", e);
        }
    }

    /**
     * Convert the specified object to a JSON String, throwing an unchecked exception (RuntimeException) if conversion fails
     *
     * @param o Object
     * @return Object as JSON
     */
    public static String toJson(@NonNull Object o) {
        try {
            return json().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object of class " + o.getClass().getName() + " to JSON", e);
        }
    }

    /**
     * Convert the specified YAML String to an object of the specified class, throwing an unchecked exception (RuntimeException) if conversion fails
     *
     * @param yaml YAML string
     * @param c    Class for the object
     * @return Object from YAML
     */
    public static <T> T fromYaml(@NonNull String yaml, @NonNull Class<T> c) {
        try {
            return yaml().readValue(yaml, c);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing YAML string to class " + c.getName(), e);
        }
    }

    /**
     * Convert the specified YAML String to an object of the specified class, throwing an unchecked exception (RuntimeException) if conversion fails
     *
     * @param json JSON string
     * @param c    Class for the object
     * @return Object from JSON
     */
    public static <T> T fromJson(@NonNull String json, @NonNull Class<T> c) {
        try {
            return json().readValue(json, c);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing JSON string to class " + c.getName(), e);
        }
    }

    /**
     * Register JSON subtypes manually. Mainly used for testing purposes.
     * In general ServiceLoader should be used for registering JSON subtypes.
     *
     * @param subTypes Subtypes to register manually
     */
    public static void registerSubtypes(@NonNull List<JsonSubType> subTypes){
        manuallyRegisteredSubtypes.addAll(subTypes);
        jsonMapper = configureMapper(new ObjectMapper());
        yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)  // For preventing YAML from adding `!<TYPE>` with polymorphic objects
                // and use Jackson's type information mechanism.
        ));
    }

    public static List<JsonSubType> getAllSubtypes(){

        ServiceLoader<JsonSubTypesMapping> sl = ServiceLoader.load(JsonSubTypesMapping.class);
        Iterator<JsonSubTypesMapping> iterator = sl.iterator();
        List<JsonSubType> out = new ArrayList<>();
        while(iterator.hasNext()){
            JsonSubTypesMapping m = iterator.next();
            List<JsonSubType> l = m.getSubTypesMapping();
            out.addAll(l);
        }

        out.addAll(manuallyRegisteredSubtypes);

        return out;
    }

    public static List<JsonSubType> getSubtypesOf(Class<?> c){
        List<JsonSubType> all = getAllSubtypes();
        List<JsonSubType> out = new ArrayList<>();
        for(JsonSubType j : all){
            if(j.getConfigInterface() == c){
                out.add(j);
            }
        }
        return out;
    }

    public static Map<Class<?>, String> getSubtypeNames(){
        List<JsonSubType> all = getAllSubtypes();
        Map<Class<?>,String> m = new HashMap<>();
        for(JsonSubType j : all){
            m.put(j.getSubtype(), j.getName());
        }
        return m;
    }

    public static JsonSubType findSubtypeByName(String name) {
        for(JsonSubType type : getAllSubtypes()) {
            if(type.getName().equals(name)){
                return type;
            }
        }
        return null;
    }
}
