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

package ai.konduit.serving.documentparser;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import org.junit.Test;
import org.nd4j.common.holder.ObjectMapperHolder;

import java.util.Arrays;

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
    public void testSerializationPython() throws Exception {
        DocumentParserStep tikaStep = new DocumentParserStep()
                .inputNames(Arrays.asList("inputDoc"))
                .outputNames(Arrays.asList("outputDoc"));

        tryDeSerialize(trySerialize(tikaStep), DocumentParserStep.class);
        testConfigSerDe(tikaStep);


    }


    private String trySerialize(Object o) throws Exception {
        return ObjectMapperHolder.getJsonMapper().writeValueAsString(o);
    }

    private <T> void tryDeSerialize(String input, Class<T> clazz) throws Exception {
        ObjectMapperHolder.getJsonMapper().readValue(input, clazz);
    }






}
