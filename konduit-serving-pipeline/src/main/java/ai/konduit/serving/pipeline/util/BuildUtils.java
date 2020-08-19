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

package ai.konduit.serving.pipeline.util;

import ai.konduit.serving.pipeline.PipelineModuleInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * Utilities for inspecting the build metadata contained in the JAR manifest, as created by the Konduit Serving
 * build tool
 *
 * @author Alex Black
 */
@Slf4j
public class BuildUtils {
    public static final String CPU = "CPU";

    private static final String MANIFEST = "META-INF/MANIFEST.MF";
    private static final String KS_BUILD = "Konduit-Serving-Build";
    private static final String TARGET = "target";
    private static final String DEVICE = "device";


    private BuildUtils(){ }

    /**
     * Read the JAR manifest (META-INF/MANIFEST.MF) as a {@code Map<String,Object>}.
     * Returns null if manifest cannot be read
     */
    public static Map<String,Object> readManifest(){
        try {
            URL u = PipelineModuleInfo.class.getClassLoader().getResource(MANIFEST);
            File manifest = new File(String.valueOf(u));
            if(!manifest.exists()){
                return null;
            }
            Map<String,Object> map = ObjectMappers.json().readValue(manifest, Map.class);
            return map;
        } catch (Throwable t){
            log.warn("Unable to read META-INF/MANIFEST.MF", t);
            return null;
        }
    }

    /**
     * Read the JAR manifest (META-INF/MANIFEST.MF) (if any) and inspect the Konduit-Serving-Build field to determine
     * the target device.<br>
     * Returns null if:<br>
     * (a) the manifest file does not exist, or<br>
     * (b) the Konduit-Serving-Build field does not exist in the manifest, or<br>
     * (c) the target field is not specified in the Konduit-Serving-Build field<br>
     * Returns "CPU" if the target field does exist but the device field is not specified.
     */
    public static String targetDevice(){
        Map<String,Object> m = readManifest();
        if(m == null || !m.containsKey(KS_BUILD)){
            return null;
        }
        Map<String,Object> m2 = (Map<String, Object>) m.get(KS_BUILD);
        Map<String,Object> target = (Map<String, Object>) m2.get(TARGET);
        if(target == null ){
            return null;
        }
        if(!target.containsKey(DEVICE)){
            return CPU;
        }

        return (String) target.get(DEVICE);
    }

    /**
     * Read the JAR manifest (META-INF/MANIFEST.MF) (if any) and inspect the Konduit-Serving-Build field to determine
     * the target device is CUDA.<br>
     * Returns false if the manifest can't be read, or if the device can't be determined.
     */
    public static boolean isCudaBuild(){
        String device = targetDevice();
        return device != null && device.toLowerCase().contains("cuda");
    }
}
