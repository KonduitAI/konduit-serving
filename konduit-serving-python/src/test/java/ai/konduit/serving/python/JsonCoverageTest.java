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

package ai.konduit.serving.python;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.model.PythonConfig;
import ai.konduit.serving.model.PythonIO;
import ai.konduit.serving.pipeline.api.data.ValueType;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.python";
    }

    @Override
    public Object fromJson(Class<?> c, String json) {
        return ObjectMappers.fromJson(json, c);
    }

    @Override
    public Object fromYaml(Class<?> c, String yaml) {
        return ObjectMappers.fromYaml(yaml, c);
    }

    @Test
    public void testPythonStep() {
        testConfigSerDe(new PythonStep().pythonConfig((PythonConfig.builder()
        .build())));
    }

    @Test
    public void testPythonIO() {
        testConfigSerDe(new PythonIO()
                .name("input")
                .pythonType("list")
                .secondaryType(ValueType.BOOLEAN));
    }

    @Test
    public void testConfigPythonIO() {
        testConfigSerDe(PythonConfig.builder().ioInput("input",PythonIO.builder()
                .name("input1").pythonType("list").secondaryType(ValueType.BOOLEAN)
                .build()).build());
    }



}
