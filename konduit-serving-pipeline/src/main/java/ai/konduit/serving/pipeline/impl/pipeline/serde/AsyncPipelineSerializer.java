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

package ai.konduit.serving.pipeline.impl.pipeline.serde;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline;
import lombok.AllArgsConstructor;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.nd4j.shade.jackson.annotation.JsonPropertyOrder;
import org.nd4j.shade.jackson.annotation.JsonUnwrapped;
import org.nd4j.shade.jackson.core.JsonGenerator;
import org.nd4j.shade.jackson.databind.JsonSerializer;
import org.nd4j.shade.jackson.databind.SerializerProvider;

import java.io.IOException;

public class AsyncPipelineSerializer extends JsonSerializer<AsyncPipeline> {

    @Override
    public void serialize(AsyncPipeline ap, JsonGenerator jg, SerializerProvider sp) throws IOException {
        Pipeline p = ap.underlying();
        AsyncPipelineSerializationHelper h = new AsyncPipelineSerializationHelper(ap.trigger(), p);
        jg.writeObject(h);
    }

    //Wrapper/helper class to inject "@AsyncTrigger" into the Pipeline JSON
    @lombok.Data
    @AllArgsConstructor
    @JsonPropertyOrder({"@type", "@input"})
    protected static class AsyncPipelineSerializationHelper {
        @JsonProperty(Data.RESERVED_KEY_ASYNC_TRIGGER)
        private Trigger _triggerAliasField_;
        @JsonUnwrapped
        private Pipeline pipeline;
    }
}
