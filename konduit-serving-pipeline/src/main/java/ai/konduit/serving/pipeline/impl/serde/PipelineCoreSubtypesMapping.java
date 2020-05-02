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
package ai.konduit.serving.pipeline.impl.serde;

import ai.konduit.serving.pipeline.api.serde.JsonSubType;
import ai.konduit.serving.pipeline.api.serde.JsonSubTypesMapping;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.step.logging.LoggingPipelineStep;

import java.util.ArrayList;
import java.util.List;

public class PipelineCoreSubtypesMapping implements JsonSubTypesMapping {


    @Override
    public List<JsonSubType> getSubTypesMapping() {
        List<JsonSubType> l = new ArrayList<>();
        l.add(new JsonSubType("LOGGING", LoggingPipelineStep.class, PipelineStep.class));

        return l;
    }
}