/*
 *  ******************************************************************************
 *  * Copyright (c) 2022 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.build.util;

import ai.konduit.serving.build.config.Module;
import ai.konduit.serving.build.steps.RunnerInfo;
import ai.konduit.serving.build.steps.StepId;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.shade.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class ModuleUtils {

    private ModuleUtils(){ }

    public static Map<StepId, List<RunnerInfo>> runnersForFile(File f){
        try {
            JsonNode jsonConfiguration = readConfiguration(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
            if(jsonConfiguration == null) {
                throw new IllegalStateException("Unable to parse string into a valid pipeline configuration from file: " + f.getAbsolutePath());
            } else {
                return runnersForJson(
                        (jsonConfiguration.has("pipeline") ?
                            jsonConfiguration.get("pipeline") :
                            jsonConfiguration)
                        .toString());
            }
        } catch (IOException e){
            throw new RuntimeException("Error reading JSON/YAML from file: " + f.getAbsolutePath(), e);
        }
    }

    /**
     * Parse the given configuration yaml/json string to {@link JsonNode}.
     *
     * @param configurationString given configuration string. Can be a JSON/YAML string
     * @return Read configuration to {@link JsonNode}. Returns null on failure.
     */
    private static JsonNode readConfiguration(String configurationString) {
        try {
            return ObjectMappers.json().readTree(configurationString);
        } catch (Exception jsonProcessingErrors) {
            try {
                return ObjectMappers.yaml().readTree(configurationString);
            } catch (Exception yamlProcessingErrors) {
                log.error("Given configuration: '{}' does not contain a valid JSON/YAML object", configurationString);
                log.error("\n\nErrors while processing as a json string:", jsonProcessingErrors);
                log.error("\n\nErrors while processing as a yaml string:", yamlProcessingErrors);
                return null;
            }
        }
    }

    public static Map<StepId, List<RunnerInfo>> runnersForJson(String json){
        //System.out.println(json);

        Map<StepId, List<RunnerInfo>> out = new HashMap<>();

        //TODO let's do this properly - this is a temporary hack for development/testing of other aspects
        Map<String,Object> map;
        try{
            map = ObjectMappers.json().readValue(json, Map.class);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        Object stepsObj = map.get("steps");
        int stepCount = 0;
        if(stepsObj instanceof List){
            List<Object> l = (List<Object>) stepsObj;
            for(Object o : l){
                if(o instanceof Map){
                    Map<String,Object> m = (Map<String,Object>)o;
                    String jsonType = (String) m.get("@type");
                    Module mod = moduleForJsonType(jsonType);
                    if(mod == null)
                        continue;

                    String runnerClass = null;      //TODO

                    String name = "";   //TODO
                    StepId id = new StepId(stepCount, name, jsonType);

                    RunnerInfo ri = new RunnerInfo(runnerClass, mod);
                    out.put(id, Collections.singletonList(ri));
                }
            }
        } else if(stepsObj instanceof Map) {
            Map<String,Object> m = (Map<String,Object>)stepsObj;
            for(Map.Entry<String,Object> e : m.entrySet()){
                if(e.getValue() instanceof Map){
                    Map<String,Object> step = (Map<String,Object>)e.getValue();
                    if(step.containsKey("@type")){
                        String jsonType = (String) step.get("@type");
                        Module mod = moduleForJsonType(jsonType);
                        if(mod == null)
                            continue;

                        String runnerClass = null;      //TODO

                        String name = "";   //TODO
                        StepId id = new StepId(stepCount, name, jsonType);

                        RunnerInfo ri = new RunnerInfo(runnerClass, mod);
                        out.put(id, Collections.singletonList(ri));
                    }
                }
            }
        }

        return out;
    }

    public static Module moduleForJsonType(String jsonType){
        Map<String,List<RunnerInfo>> map = jsonNameToRunnerClass();
        if(!map.containsKey(jsonType)){
            log.warn("No JSON subtype known for: {} (safe to ignore for custom pipeline steps)", jsonType);
            return null;
        }

        List<RunnerInfo> l = map.get(jsonType);
        if(l == null || l.isEmpty()){
            log.warn("Failed to determine runner for JSON type {} - class represents custom functionality, or missing @CanRun annotation on the runner?", jsonType);
            return null;
        }

        if(l.size() > 1){
            log.warn("More than 1 runner available for JSON type {} - returning first", jsonType);
        }
        if (l.get(0) == null) {
            log.warn("Failed to determine runner for JSON type {} - class represents custom functionality, or missing @CanRun annotation on the runner?", jsonType);
            return null;
        }

        return l.get(0).module();
    }

    public static Map<String,RunnerInfo> pipelineClassToRunnerClass(){
        String s;
        try {
            File f = new ClassPathResource("META-INF/konduit-serving/PipelineStepRunner").getFile();
            s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        String[] lines = s.split("\n");
        Map<String,RunnerInfo> out = new HashMap<>();
        for(String line : lines){
            String[] split = line.split(",");       //Format: pipelineClass,runnerClass,module - i.e., "this type of pipeline step (in specified module) can be run by this type of runner"
            RunnerInfo info = new RunnerInfo(split[1], Module.forName(split[2]));
            out.put(split[0], info);
        }
        return out;
    }

    public static Map<String,List<RunnerInfo>> jsonNameToRunnerClass(){
        String s;
        try {
            File f = new ClassPathResource("META-INF/konduit-serving/JsonNameMapping").getFile();
            s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        Map<String,RunnerInfo> c2Runner = pipelineClassToRunnerClass();

        String[] lines = s.split("\n");
        Map<String,List<RunnerInfo>> out = new HashMap<>();
        for(String line : lines){
            if(line.isEmpty())
                continue;
            String[] split = line.split(",");            //Format: json_name,class_name,interface_name
            RunnerInfo info = c2Runner.get(split[1]);
            List<RunnerInfo> l = out.computeIfAbsent(split[0], k -> new ArrayList<>());
            l.add(info);
        }
        return out;
    }

}
