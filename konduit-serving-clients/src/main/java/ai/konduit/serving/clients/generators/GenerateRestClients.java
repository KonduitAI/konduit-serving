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
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.ClientOpts;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.generators.java.JavaClientCodegen;
import io.swagger.codegen.v3.generators.python.PythonClientCodegen;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;

@Slf4j
public class GenerateRestClients {

    public static void main(String[] args) throws NotFoundException, IOException {
        System.out.println("Classpath: " + System.getProperty("java.class.path"));
        // Setting this so that the Json Serializer is able see private fields without standard getter methods.
        Json.mapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        Map<String, List<Pair<String, String>>> mappings = getJsonNameMappings();
        mappings.put("ai.konduit.serving.endpoint.Endpoint",
                Collections.singletonList(new Pair<>(null, "ai.konduit.serving.endpoint.AssetServingEndpoint"))
        );
        mappings.put("ai.konduit.serving.pipeline.api.data.BoundingBox",
                Arrays.asList(new Pair<>(null, "ai.konduit.serving.pipeline.impl.data.box.BBoxCHW"),
                        new Pair<>(null, "ai.konduit.serving.pipeline.impl.data.box.BBoxXY"))
        );
        mappings.put("ai.konduit.serving.pipeline.api.pipeline.Pipeline",
                Arrays.asList(new Pair<>(null, "ai.konduit.serving.pipeline.impl.pipeline.SequencePipeline"),
                        new Pair<>(null, "ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline"))
        );

        ClassPool classPool = ClassPool.getDefault();

        OpenAPI openAPI = new OpenAPI();
        createApiInfo(openAPI);
        List<CtClass> annotatedClasses = createAnnotatedClasses(classPool, mappings);
        annotatedClasses.add(classPool.get("ai.konduit.serving.vertx.config.InferenceConfiguration"));
        annotatedClasses.add(classPool.get("ai.konduit.serving.vertx.protocols.http.api.ErrorResponse"));

        // This has to be done in order otherwise we'll have duplicated classes compilation error.
        // This is to make sure that the classes referenced in the later iterations are defined in the previous one.
        try {
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.endpoint.Endpoint")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.api.data.BoundingBox")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.api.step.PipelineStep")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.impl.pipeline.graph.SwitchFn")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.pipeline.api.pipeline.Pipeline")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.vertx.config.InferenceConfiguration")).toClass());
            addSchemas(openAPI, annotatedClasses.get(findIndex(annotatedClasses, "ai.konduit.serving.vertx.protocols.http.api.ErrorResponse")).toClass());
        } catch (CannotCompileException e) {
            log.error("Error while adding schema classes to OpenApi specs", e);
            System.exit(1);
        }

        log.info("Generated open api spec is: \n{}\n", Yaml.pretty(openAPI));
        generateClients(openAPI);
    }

    private static void generateClients(OpenAPI openAPI) throws IOException {
        String clientsSavePath = System.getProperty("konduit.generator.clients.directory");
        File clientsDirectory = new File(clientsSavePath == null ? "clients" : clientsSavePath);
        log.info("Generating clients at: {}", clientsDirectory.getAbsolutePath());

        try {
            if (clientsDirectory.exists() && clientsDirectory.isDirectory())
                FileUtils.deleteDirectory(clientsDirectory);
        } catch (IOException exception) {
            log.error("Unable to clean 'clients' directory at {}", clientsDirectory.getAbsolutePath(), exception);
            System.exit(1);
        }

        DefaultGenerator defaultGenerator = new DefaultGenerator();

        JavaClientCodegen javaClientCodegen = new JavaClientCodegen();
        javaClientCodegen.setOutputDir(new File(clientsDirectory, "java").getAbsolutePath());
        javaClientCodegen.setModelPackage("ai.konduit.serving.client.java.models");
        javaClientCodegen.setInvokerPackage("ai.konduit.serving.client.java.invoker");
        javaClientCodegen.setApiPackage("ai.konduit.serving.client.java");
        javaClientCodegen.setGroupId("ai.konduit.serving");
        javaClientCodegen.setArtifactId("konduit-serving-client");
        javaClientCodegen.setArtifactVersion("0.1.0-SNAPSHOT");
        javaClientCodegen.setTemplateDir("konduit-client-templates/Java");


        List<File> generatedJavaClientFiles = defaultGenerator
                .opts(new ClientOptInput()
                        .openAPI(openAPI)
                        .config(javaClientCodegen)
                        .opts(new ClientOpts()))
                .generate();

        PythonClientCodegen pythonClientCodegen = new PythonClientCodegen();
        pythonClientCodegen.setOutputDir(new File(clientsDirectory, "python").getAbsolutePath());
        pythonClientCodegen.setTemplateDir("konduit-client-templates/python");

        ClientOpts pythonClientOpts = new ClientOpts();
        pythonClientOpts.getProperties().put(CodegenConstants.PACKAGE_NAME, "konduit");
        pythonClientOpts.getProperties().put(CodegenConstants.PACKAGE_VERSION, "0.2.0"); // new version after already available "konduit" version on PyPi (which is 0.1.10) - https://pypi.org/project/konduit/0.1.10/

        List<File> generatedPythonClientFiles = defaultGenerator
                .opts(new ClientOptInput()
                        .openAPI(openAPI)
                        .config(pythonClientCodegen)
                        .opts(pythonClientOpts))
                .generate();

        findAndReplaceCharacters(generatedJavaClientFiles);
        findAndReplaceCharacters(generatedPythonClientFiles);
    }

    private static void findAndReplaceCharacters(List<File> generatedFiles) throws IOException {
        log.info("\n\nReplacing new line characters in the generated files: ");
        for(File file : generatedFiles) {
            if(file.getAbsolutePath().endsWith(".md") || file.getAbsolutePath().endsWith(".java")) {
                replace(file, "&lt;br&gt;", "<br>");
            }

            if(file.getAbsolutePath().endsWith(".md")) {
                replace(file, "&quot;", "\"");
                replace(file, "&lt;", "<");
                replace(file, "&gt;", ">");
            }

            if(file.getAbsolutePath().endsWith(".py")) {
                replace(file, "<br>", "\n\t\t");
            }
        }
    }

    private static String escape(String input) {
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static void replace(File file, String target, String replacement) throws IOException {
        replace(file, target, replacement, true);
    }

    private static void replace(File file, String target, String replacement, boolean showMessage) throws IOException {
        FileUtils.writeStringToFile(file,
                FileUtils.readFileToString(file, StandardCharsets.UTF_8).replace(target, replacement),
                StandardCharsets.UTF_8);

        if(showMessage) {
            log.info("Replaced {} to {} in {}", escape(target), escape(replacement), file.getAbsolutePath());
        }
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
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(new ClassPathResource(resourcePath).getFile()))) {
            Map<String, List<Pair<String, String>>> mappings = new LinkedHashMap<>();
            while (true) {
                String line = bufferedReader.readLine();
                if(line == null) {
                    break;
                } else {
                    line = line.trim();
                }

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

    private static void createApiInfo(OpenAPI openAPI) {
        try (InputStream is = GenerateRestClients.class.getClassLoader().getResourceAsStream("META-INF/konduit-serving-clients-git.properties")) {
            if (is == null) {
                throw new IllegalStateException("Cannot find konduit-serving-clients-git.properties on classpath");
            }
            Properties gitProperties = new Properties();
            gitProperties.load(is);
            String projectVersion = gitProperties.getProperty("git.build.version");
            String commitId = gitProperties.getProperty("git.commit.id").substring(0, 8);

            openAPI.info(new Info()
                    .title("Konduit Serving REST API")
                    .version(String.format("%s | Commit: %s", projectVersion, commitId))
                    .description("RESTful API for various operations inside konduit-serving")
                    .license(new License()
                            .name("Apache 2.0")
                            .url("https://github.com/KonduitAI/konduit-serving/blob/master/LICENSE"))
                    .contact(new Contact()
                            .url("https://konduit.ai/contact")
                            .name("Konduit K.K.")
                            .email("hello@konduit.ai")))
                    .tags(Collections.singletonList(
                            new Tag()
                                    .name("inference")
                                    .description("Inference server operations")))
                    .externalDocs(new ExternalDocumentation()
                            .description("Online documentation")
                            .url("https://serving.konduit.ai"))
                    .path("/predict", new PathItem()
                            .summary("Predicts an output based on the given JSON (key/value) or binary string")
                            .description("Takes a JSON string of key value pairs or a binary data string (protobuf) as input " +
                                    "and processes it in the pipeline. The output could be json or a binary string based on " +
                                    "the accept header value (application/json or application/octet-stream respectively).")
                            .post(new Operation()
                                    .operationId("predict")
                                    .addTagsItem("inference")
                                    .requestBody(new RequestBody()
                                            .required(true)
                                            .content(new Content()
                                                    .addMediaType(APPLICATION_JSON.toString(),
                                                            new MediaType().schema(new MapSchema()))
                                                    .addMediaType(APPLICATION_OCTET_STREAM.toString(),
                                                            new MediaType().schema(new BinarySchema()))
                                            )
                                    ).responses(new ApiResponses()
                                            .addApiResponse("200", new ApiResponse()
                                                    .description("Successful operation")
                                                    .content(new Content()
                                                            .addMediaType(APPLICATION_JSON.toString(),
                                                                    new MediaType().schema(new MapSchema()))
                                                            .addMediaType(APPLICATION_OCTET_STREAM.toString(),
                                                                    new MediaType().schema(new BinarySchema()))
                                                    )
                                            ).addApiResponse("500", new ApiResponse()
                                                    .description("Internal server error")
                                                    .content(new Content()
                                                            .addMediaType(APPLICATION_JSON.toString(), new MediaType()
                                                                    .schema(new ObjectSchema().$ref("#/components/schemas/ErrorResponse"))
                                                            )
                                                    )
                                            )
                                    )
                            )
                    );
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
