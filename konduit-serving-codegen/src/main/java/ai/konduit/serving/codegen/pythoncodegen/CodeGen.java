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
import ai.konduit.serving.pipeline.BasePipelineStep;
import ai.konduit.serving.pipeline.PipelineStep;
import ai.konduit.serving.pipeline.config.NormalizationConfig;
import ai.konduit.serving.pipeline.config.ObjectDetectionConfig;
import ai.konduit.serving.pipeline.step.*;
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
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema());
        Class<?>[] clazzes = {
                TensorDataTypesConfig.class,
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
                BasePipelineStep.class,
                NormalizationConfig.class,
                PythonStep.class,
                TransformProcessStep.class,
                ModelStep.class,
                ArrayConcatenationStep.class,
                JsonExpanderTransformStep.class,
                ImageLoadingStep.class,
                MemMapConfig.class,
                InferenceConfiguration.class,
        };


        String sep = File.separator;

        String codeGenBasePath = System.getProperty("user.dir");
        String projectBasePath = codeGenBasePath.replace(sep + "model-server-codegen", "");

        File newModule = new File(
                projectBasePath + sep + "python" + sep + "konduit" + sep + "base_inference.py");
        boolean moduleDeleted = newModule.delete();
        System.out.println(moduleDeleted);
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
                boolean deleted = classJson.delete();
                System.out.println(deleted);
            }
            FileUtils.writeStringToFile(classJson, objectMapper.writeValueAsString(jsonNode), Charset.defaultCharset());
            File pythonFile = new File(String.format(projectBasePath + sep + "python"
                    + sep + "%s.py", clazz.getSimpleName().toLowerCase()));

            Process p = runtime.exec(String.format("jsonschema2popo -o %s %s\n", pythonFile.getAbsolutePath(),
                    classJson.getAbsolutePath())
            );
            p.waitFor(10, TimeUnit.SECONDS);
            if (p.exitValue() != 0) {
                String errorMessage = "";
                try (InputStream is = p.getInputStream()) {
                    errorMessage += IOUtils.toString(is, Charset.defaultCharset());

                }
                throw new IllegalStateException("Json schema conversion in python threw an error with output "
                        + errorMessage);
            }
            p.destroy();

            //change class names
            String load = FileUtils.readFileToString(pythonFile, Charset.defaultCharset());
            if(PipelineStep.class.isAssignableFrom(clazz) && !clazz.equals(PipelineStep.class))
                load = load.replaceFirst(replace.pattern(),"\nclass "
                        + clazz.getSimpleName() + "(PipelineStep):");
            else
                load = load.replaceFirst(replace.pattern(),"\nclass "
                        + clazz.getSimpleName() + "(object):");

            //change keywords args to underscores
            StringBuilder kwArgsAsUnderScore = new StringBuilder();
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
                    StringBuilder newSplitLine = new StringBuilder();
                    newSplitLine.append(split2[0]);
                    newSplitLine.append(" = ");
                    String changed = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, split2[1]);
                    newSplitLine.append(changed);
                    splitLine = newSplitLine.toString();
                }

                splitLine = splitLine.replace("_none","None");
                kwArgsAsUnderScore.append(splitLine).append("\n");
            }

            load = kwArgsAsUnderScore.toString();
            load = load.replace("d = dict()","d = empty_type_dict(self)");
            //load = load.replaceFirst("import enum",imports.toString());
            FileUtils.writeStringToFile(newModule,load,Charset.defaultCharset(),true);

            // Clean up JSON files after code generation.
            boolean pythonDeleted = pythonFile.delete();
            System.out.println(pythonDeleted);
            if(classJson.exists()) {
                boolean deleteStatus = classJson.delete();
                System.out.println(classJson.toString() + " intermediate JSON file was deleted "
                        + (deleteStatus ? "successfully" : "unsuccessfully"));
            }
        }

        String loadedModule = FileUtils.readFileToString(newModule, Charset.defaultCharset());

        loadedModule = applyPythonWrappers(loadedModule);
        loadedModule = patchPythonDefaultValues(loadedModule);
        loadedModule = PythonDocStrings.generateDocs(loadedModule);

        String sb = "import enum\nfrom konduit.json_utils import empty_type_dict,DictWrapper,ListWrapper\n" +
                loadedModule;

        FileUtils.writeStringToFile(newModule, sb,Charset.defaultCharset(),false);

        Process autopepLinting = runtime.exec("autopep8 --in-place " + newModule);
        autopepLinting.waitFor(8, TimeUnit.SECONDS);
        if(autopepLinting.exitValue() != 0) {
            String errorMessage = "";
            try(InputStream is = autopepLinting.getInputStream()) {
                errorMessage += IOUtils.toString(is,Charset.defaultCharset());

            }
            throw new IllegalStateException("Code linting failed with error message: "+ errorMessage);
        }
        autopepLinting.destroy();

        Process blackLinting = runtime.exec("black " + newModule);
        blackLinting.waitFor(5, TimeUnit.SECONDS);
        if(blackLinting.exitValue() != 0) {
            String errorMessage = "";
            try(InputStream is = blackLinting.getInputStream()) {
                errorMessage += IOUtils.toString(is,Charset.defaultCharset());

            }
            throw new IllegalStateException("Code linting failed with error message: "+ errorMessage);
        }
        blackLinting.destroy();
    }

    private static String applyPythonWrappers(String loadedModule) {
        loadedModule = loadedModule.replace("import enum","");
        loadedModule = loadedModule.replace("#!/usr/bin/env/python","");
        loadedModule = loadedModule.replace("def __init__(self\n" +
                "            ):","def __init__(self):\n\t\tpass");
        loadedModule = loadedModule.replace("if not isinstance(value, type)",
                "if not isinstance(value, dict) and not isinstance(value, DictWrapper)");
        loadedModule = loadedModule.replace("if not isinstance(value, type)",
                "if not isinstance(value, list) and not isinstance(value, ListWrapper)");
        loadedModule = loadedModule.replace("if not isinstance(value, dict)",
                "if not isinstance(value, dict) and not isinstance(value, DictWrapper)");
        loadedModule = loadedModule.replace("if not isinstance(value, list)",
                "if not isinstance(value, list) and not isinstance(value, ListWrapper)");
        loadedModule = loadedModule.replace("'type': type","'type': dict");
        return loadedModule;
    }

    /**
     * Modify some constructor defaults to leverage Python's strengths
     * By default we work with numpy-in-numpy-out and "raw" predictions to cause minimal harm to the intended
     * audience.
     * @param loadedModule String containing all Python code up to this point
     * @return patched Python code
     */
    private static String patchPythonDefaultValues(String loadedModule) {

        // Input and output formats
        loadedModule = loadedModule.replace("input_data_format=None", "input_data_format='NUMPY'");
        loadedModule = loadedModule.replace("output_data_format=None", "output_data_format='NUMPY'");
        loadedModule = loadedModule.replace("prediction_type=None", "prediction_type='RAW'");

        loadedModule = loadedModule.replace("setup_and_run=None", "setup_and_run=False");


        loadedModule = loadedModule.replace("uploads_directory=None", "uploads_directory='file-uploads/'");

        return loadedModule;
    }
}