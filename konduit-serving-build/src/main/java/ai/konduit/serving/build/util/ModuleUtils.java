/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleUtils {

    private ModuleUtils(){ }

    public static Map<StepId, List<RunnerInfo>> runnersForFile(File f){
        //First determine if JSON or YAML...
        boolean json = true;    //TODO
        try {
            if (json) {
                return runnersForJson(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
            } else {
                return runnersForYaml(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
            }
        } catch (IOException e){
            throw new RuntimeException("Error reading JSON/YAML from file: " + f.getAbsolutePath(), e);
        }
    }

    public static Map<StepId, List<RunnerInfo>> runnersForJson(String json){
        System.out.println(json);

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
                    String runnerClass = null;      //TODO

                    String name = "";   //TODO
                    StepId id = new StepId(stepCount, name, jsonType);

                    RunnerInfo ri = new RunnerInfo(runnerClass, mod);
                    out.put(id, Collections.singletonList(ri));
                }
            }
        } else {
            throw new UnsupportedOperationException("GraphPipeline handling not yet implemented");
        }

        return out;
    }

    public static Map<StepId, List<RunnerInfo>> runnersForYaml(String yaml){
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static Module moduleForJsonType(String jsonType){
        //TODO we'll also do this properly - again, just a temporary hack
        //Not hardcoded here, properly extensible, etc
        switch (jsonType){
            case "DEEPLEARNING4J":
                return Module.DL4J;
            case "SAMEDIFF":
                return Module.SAMEDIFF;
            default:
                throw new RuntimeException("Not implemented module mapping for: " + jsonType);
        }
    }

}
