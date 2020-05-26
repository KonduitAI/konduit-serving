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

import ai.konduit.serving.build.steps.RunnerInfo;
import ai.konduit.serving.build.steps.StepId;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static Map<StepId, List<RunnerInfo>> runnersForYaml(String yaml){
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
