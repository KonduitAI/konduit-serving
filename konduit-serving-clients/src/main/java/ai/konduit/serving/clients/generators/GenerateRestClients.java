/*
 * *****************************************************************************
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
 * ****************************************************************************
 */

package ai.konduit.serving.clients.generators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.common.primitives.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GenerateRestClients {

    public static void main(String[] args) throws NotFoundException, IOException {
        // Setting this so that the Json Serializer is able see private fields without standard getter methods.
        Json.mapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        Map<String, List<Pair<String, String>>> mappings = getJsonNameMappings();
        mappings.put("ai.konduit.serving.pipeline.api.pipeline.Pipeline",
                Arrays.asList(new Pair<>(null, "ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline"),
                        new Pair<>(null, "ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline"))
        );

        ClassPool classPool = ClassPool.getDefault();

        OpenAPI openAPI = new OpenAPI();
        List<CtClass> annotatedClasses = createAnnotatedClasses(classPool, mappings);
        annotatedClasses.add(classPool.get("ai.konduit.serving.vertx.config.InferenceConfiguration"));

        // This has to be done in order otherwise we'll have duplicated classes compilation error.
        // This is to make sure that the classes referenced in the later iterations are defined in the previous one.
        try {
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.api.step.PipelineStep")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.api.pipeline.Pipeline")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.vertx.config.InferenceConfiguration")).toClass());
        } catch (CannotCompileException e) {
            log.error("Error while adding schema classes to OpenApi specs", e);
            System.exit(1);
        }

        log.info(Yaml.pretty(openAPI));
    }

    private static int findIndex(List<CtClass> array, String className) {
        for(int i = 0; i < array.size(); i++) {
            if(array.get(i).getName().equals(className)) {
                return i;
            }
        }

        return -1;
    }

    private static Map<String, List<Pair<String, String>>> getJsonNameMappings() throws IOException {
        String resourcePath = "META-INF/konduit-serving/JsonNameMapping";
        try {
            String jsonNameMappingsString = FileUtils.readFileToString(new ClassPathResource(resourcePath).getFile(),
                    StandardCharsets.UTF_8);

            Map<String, List<Pair<String, String>>> mappings = new LinkedHashMap<>();
            for(String line : jsonNameMappingsString.split(System.lineSeparator())) {
                String[] splits = line.split(",");
                if(splits.length > 2) {
                    String key = splits[2]; // Super class
                    Pair<String, String> value = new Pair<>(splits[0],  splits[1]); // (Type, sub type class)
                    if(mappings.containsKey(key)) {
                        mappings.get(key).add(value);
                    } else {
                        mappings.put(key, new ArrayList<>(Collections.singleton(value)));
                    }
                }
            }

            return mappings;
        } catch (FileNotFoundException exception) {
            log.error("Couldn't find file: {}. Installing 'konduit-serving-meta' module might fix this.", resourcePath);
            System.exit(1);
        }

        return null;
    }

    private static List<CtClass> createAnnotatedClasses(ClassPool classPool, Map<String, List<Pair<String, String>>> mappings) {
        return mappings.entrySet().stream().map(
                entry -> {
                    String superClass = entry.getKey();
                    List<Pair<String, String>> jsonNamesAndClasses = entry.getValue();

                    CtClass ctClass;
                    try {
                         ctClass = classPool.get(superClass);
                    } catch (NotFoundException e) {
                        log.error("Couldn't create annotated classes from the given inputs", e);
                        System.exit(1);
                        return null;
                    }

                    ClassFile classFile = ctClass.getClassFile();
                    ConstPool constPool = classFile.getConstPool();
                    AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
                    Annotation annotation = new Annotation("io.swagger.v3.oas.annotations.media.Schema", constPool);
                    ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
                    arrayMemberValue.setValue(jsonNamesAndClasses.stream()
                            .map(jsonNameAndClass -> new ClassMemberValue(jsonNameAndClass.getValue(), constPool)).toArray(ClassMemberValue[]::new)
                    );

                    // Add discriminator and their mappings for polymorphism if their json type names aren't null
                    if(jsonNamesAndClasses.get(0).getKey() != null) {
                        annotation.addMemberValue("discriminatorProperty", new StringMemberValue("@type", constPool));
                        ArrayMemberValue discriminatorMappingArray = new ArrayMemberValue(constPool);
                        discriminatorMappingArray.setValue(jsonNamesAndClasses.stream()
                                .map(jsonNameAndClass -> {
                                    Annotation discriminatorMappingAnnotation = new Annotation("io.swagger.v3.oas.annotations.media.DiscriminatorMapping", constPool);
                                    discriminatorMappingAnnotation.addMemberValue("value", new StringMemberValue(jsonNameAndClass.getKey(), constPool));
                                    discriminatorMappingAnnotation.addMemberValue("schema", new ClassMemberValue(jsonNameAndClass.getValue(), constPool));
                                    return new AnnotationMemberValue(discriminatorMappingAnnotation, constPool);
                                }).toArray(AnnotationMemberValue[]::new)
                        );
                        annotation.addMemberValue("discriminatorMapping", discriminatorMappingArray);
                    }

                    // Ignore the graph builder for GraphStep
                    if(superClass.equals("ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep")) {
                        Annotation jsonIgnorePropertyAnnotation = new Annotation("com.fasterxml.jackson.annotation.JsonIgnoreProperties", constPool);
                        ArrayMemberValue ignoredPropertiesValue = new ArrayMemberValue(constPool);
                        ignoredPropertiesValue.setValue(new StringMemberValue[] { new StringMemberValue("builder", constPool) });
                        jsonIgnorePropertyAnnotation.addMemberValue("value", ignoredPropertiesValue);
                        annotationsAttribute.addAnnotation(jsonIgnorePropertyAnnotation);
                    }

                    annotation.addMemberValue("subTypes", arrayMemberValue);
                    annotationsAttribute.addAnnotation(annotation);
                    ctClass.getClassFile().addAttribute(annotationsAttribute);
                    return ctClass;
                }
        ).collect(Collectors.toList());
    }

    private static void addSchemas(OpenAPI openAPI, Class<?> clazz) {
        ModelConverters.getInstance().readAll(clazz).forEach(openAPI::schema);
    }
}
