/* ******************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/
package ai.konduit.serving.deeplearning4j.serde;

import ai.konduit.serving.deeplearning4j.DL4JConfiguration;
import ai.konduit.serving.deeplearning4j.step.DL4JModelPipelineStep;
import ai.konduit.serving.pipeline.api.Configuration;
import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;

import java.util.ArrayList;
import java.util.List;

public class DL4JJsonSubTypesMapping implements JsonSubTypesMapping {


    @Override
    public List<JsonSubType> getSubTypesMapping() {

        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("DEEPLEARNING4J", DL4JModelPipelineStep.class, PipelineStep.class));
        l.add(new JsonSubType("DEEPLEARNING4J_CONFIG", DL4JConfiguration.class, Configuration.class));

        return l;
    }
}
