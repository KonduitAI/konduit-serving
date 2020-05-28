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

package ai.konduit.serving.build;

import org.apache.commons.io.FileUtils;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.common.resources.Resources;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Temp {

    public static void main(String[] args) throws Exception {
//        File f = Resources.asFile("META-INF/konduit-serving/ai.konduit.serving.annotations.CanRun");
//        File f = new ClassPathResource("META-INF/konduit-serving/ai.konduit.serving.annotation.CanRun").getFile();
        File f = new ClassPathResource("META-INF/konduit-serving/PipelineStepRunnerMeta").getFile();
        System.out.println(f.getAbsolutePath());
        String s = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        System.out.println(s);
    }

}
