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
package ai.konduit.serving.miscutils;

import org.apache.commons.io.IOUtils;
import org.nd4j.linalg.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PythonPathUtils {

    public static String getPythonPath() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("python",
                new ClassPathResource("scripts/python_path.py").getFile().getAbsolutePath());
        builder.redirectErrorStream(true);

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(builder.start().getInputStream()))) {
            return IOUtils.toString(bufferedReader);
        }
    }
}
