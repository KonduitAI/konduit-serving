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
package ai.konduit.serving.vertx;

import ai.konduit.serving.common.test.BaseJsonCoverageTest;
import ai.konduit.serving.pipeline.impl.pipeline.GraphPipeline;
import ai.konduit.serving.pipeline.impl.pipeline.graph.GraphStep;
import ai.konduit.serving.pipeline.util.ObjectMappers;
import ai.konduit.serving.vertx.config.InferenceConfiguration;
import ai.konduit.serving.vertx.config.ServerProtocol;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JsonCoverageTest extends BaseJsonCoverageTest {

    @Override
    public String getPackageName() {
        return "ai.konduit.serving.vertx";
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
    public void InferenceConfiguration(){
        Map<String, GraphStep> steps = new HashMap<String, GraphStep>();
       testConfigSerDe(new InferenceConfiguration()
               .customEndpoints(Collections.singletonList("0"))
               .port(80).protocol(ServerProtocol.HTTP)
               .pipeline(new GraphPipeline(steps, "foo", "myGraphPipeline"))
       );
    }


}
