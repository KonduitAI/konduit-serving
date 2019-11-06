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

package ai.konduit.serving.codegen.pythoncodegen;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.*;
import ai.konduit.serving.model.*;
import ai.konduit.serving.pipeline.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Konduit Python client generator
 *
 */
public class CodeGen {
    public static void main( String[] args ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper,JsonSchemaConfig.html5EnabledSchema());
        Class<?>[] clazzes = {
                TensorDataTypesConfig.class,
                PubsubConfig.class,
                SavedModelConfig.class,
                ParallelInferenceConfig.class,
                ModelConfigType.class,
                ModelConfig.class,
                TensorDataType.class,
                PmmlConfig.class,
                ObjectDetectionConfig.class,
                SchemaType.class,
                Input.class,
                Output.class,
                SameDiffConfig.class,
                TensorFlowConfig.class,
                PythonConfig.class,
                ServingConfig.class,
                PipelineStep.class,
                NormalizationConfig.class,
                PythonPipelineStep.class,
                TransformProcessPipelineStep.class,
                ModelPipelineStep.class,
                ArrayConcatenationStep.class,
                JsonExpanderTransform.class,
                ImageLoading.class,
                InferenceConfiguration.class
        };


        String sep = File.separator;

        String codegenBasePath = System.getProperty("user.dir");
        String projectBasePath = codegenBasePath.replace(sep + "model-server-codegen", "");

        StringBuffer pythonImports = new StringBuffer();
        pythonImports.append("import enum\n");
        pythonImports.append("from konduit.json_utils import empty_type_dict,DictWrapper,ListWrapper\n");

        File newModule = new File( projectBasePath + sep + "python" + sep + "konduit" + sep + "base_inference.py");
        newModule.delete();
        Runtime runtime = Runtime.getRuntime();
        Pattern replace = Pattern.compile("class\\s[A-Za-z]+:");

        for(Class<?> clazz : clazzes) {
            System.out.println("Writing class " + clazz.getSimpleName());
            JsonNode jsonNode = jsonSchemaGenerator.generateJsonSchema(clazz);
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode.putObject("definitions");
            objectNode.put("title",clazz.getSimpleName());
            File classJson = new File("schema-%s.json", clazz.getSimpleName());
            if(classJson.exists()) {
                classJson.delete();
            }
            FileUtils.writeStringToFile(classJson, objectMapper.writeValueAsString(jsonNode), Charset.defaultCharset());
            File pythonFile = new File(String.format(projectBasePath + sep + "python" + sep +"%s.py",clazz.getSimpleName().toLowerCase()));
            StringBuffer command = new StringBuffer();

            command.append(String.format("jsonschema2popo -o %s %s\n", pythonFile.getAbsolutePath(), classJson.getAbsolutePath())); // schemaJsonFile
            Process p = runtime.exec(command.toString());
            p.waitFor(10, TimeUnit.SECONDS);
            if(p.exitValue() != 0) {
                String errorMessage = "";
                try(InputStream is = p.getInputStream()) {
                    errorMessage += IOUtils.toString(is,Charset.defaultCharset());

                }
                throw new IllegalStateException("Json schema conversion in python threw an error with output " + errorMessage);
            }
            p.destroy();

            //change class names
            String load = FileUtils.readFileToString(pythonFile, Charset.defaultCharset());
            if(PipelineStep.class.isAssignableFrom(clazz) && !clazz.equals(PipelineStep.class))
                load = load.replaceFirst(replace.pattern(),"\nclass " + clazz.getSimpleName() + "(PipelineStep):");
            else
                load = load.replaceFirst(replace.pattern(),"\nclass " + clazz.getSimpleName() + "(object):");

            //change keywords args to underscores
            StringBuffer kwArgsAsUnderScore = new StringBuffer();
            String[] split = load.split("\n");
            for(String splitLine : split) {
                if(splitLine.contains("=None")) {
                    splitLine = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, splitLine);
                }
                //property needed for json
                else if(!splitLine.contains("'")
                        && !splitLine.contains("TypeError")
                        && !splitLine.contains("ValueError")
                        && !splitLine.contains("class")
                        && !splitLine.contains("isinstance")
                        && !splitLine.contains("enum")){
                    splitLine = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, splitLine);

                }
                else if(splitLine.contains("'") && splitLine.contains("=") && !splitLine.contains("enum")) {
                    String[] split2 = splitLine.split("=");
                    StringBuffer newSplitLine = new StringBuffer();
                    newSplitLine.append(split2[0]);
                    newSplitLine.append(" = ");
                    String changed = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, split2[1]);
                    newSplitLine.append(changed);
                    splitLine = newSplitLine.toString();
                }

                splitLine = splitLine.replace("_none","None");
                kwArgsAsUnderScore.append(splitLine + "\n");
            }

            load = kwArgsAsUnderScore.toString();
            load = load.replace("d = dict()","d = empty_type_dict(self)");
            //load = load.replaceFirst("import enum",imports.toString());
            FileUtils.writeStringToFile(newModule,load,Charset.defaultCharset(),true);

            // Clean up JSON files after code generation.
            pythonFile.delete();
            if(classJson.exists()) {
                boolean deleteStatus = classJson.delete();
                System.out.println(classJson.toString() + " intermediate JSON file was deleted " + (deleteStatus ? "successfully" : "unsuccessfully"));            }
        }

        String loadedModule = FileUtils.readFileToString(newModule, Charset.defaultCharset());
        loadedModule = loadedModule.replace("import enum","");
        loadedModule = loadedModule.replace("#!/usr/bin/env/python","");
        loadedModule = loadedModule.replace("def __init__(self\n" +
                "            ):","def __init__(self\n" +
                "            ):\npass");
        loadedModule = loadedModule.replace("if not isinstance(value, type)","if not isinstance(value, dict) and not isinstance(value,DictWrapper)");
        loadedModule = loadedModule.replace("if not isinstance(value, type)","if not isinstance(value, list) and not isinstance(value,ListWrapper)");
        loadedModule = loadedModule.replace(" if not isinstance(value, dict)"," if not isinstance(value, dict) and not isinstance(value,DictWrapper)");
        loadedModule = loadedModule.replace(" if not isinstance(value, list)"," if not isinstance(value, list) and not isinstance(value,ListWrapper)");
        loadedModule = loadedModule.replace("'type': type","'type': dict");
        StringBuffer sb = new StringBuffer();
        sb.append("import enum\n");
        sb.append("from konduit.json_utils import empty_type_dict,DictWrapper,ListWrapper\n");
        
        //dictionary wrapper for serialization
        sb.append(loadedModule);
        FileUtils.writeStringToFile(newModule, sb.toString(),Charset.defaultCharset(),false);


    }
}