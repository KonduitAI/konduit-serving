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

package ai.konduit.serving.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;

/**
 * Python configuration for specifying:
 *   1. pythonCode: actual python source code
 *   2. pythonCodePath: a path to a source file.
 *   3. pythonPath: a python path for dependencies
 *   4. pythonInputs/pythonOutputs/extraInputs: a map of variable name to python type
 *   5. returnAllInputs: rather than specifying outputs explicitly, the python execution
 *   will just return all created python variables during execution
 *
 * @author Adam Gibson
 */
@Data
@NoArgsConstructor
@Slf4j
@SuperBuilder
public class PythonConfig implements Serializable {

    private String  pythonCode, pythonCodePath, pythonPath;

    @Singular
    private Map<String,String>  pythonInputs, pythonOutputs, extraInputs;

    private boolean returnAllInputs;

    private boolean setupAndRun;

    private static String defaultPythonPath;


}
