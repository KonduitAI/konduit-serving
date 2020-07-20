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

package ai.konduit.serving.pipeline.impl.pipeline;

import ai.konduit.serving.pipeline.api.pipeline.Trigger;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import ai.konduit.serving.pipeline.impl.pipeline.serde.AsyncPipelineSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.nd4j.shade.jackson.databind.annotation.JsonSerialize;

/**
 * AsyncPipeline is used for situations such as processing streams of data. The idea is that an AsyncPipeline
 * may perform execution in the background (internally); when the AsyncPipeline is queried, it may return the last processed
 * Data output. The behaviour of the AsyncPipeline is determined by the underlying Trigger.<br>
 * Note that because of the asynchronous nature of AsyncPipeline, the input Data instance may not actually be used
 * when executing the underlying pipeline. This means that AsyncPipeline is restricted to situations where the Data is loaded within
 * the pipeline itself, or no (external) input is required. For example, when an image (video frame) is loaded by FrameCaptureStep.
 *
 * @author Alex Black
 */
@Data
@Accessors(fluent = true)
@JsonSerialize(using = AsyncPipelineSerializer.class)
@Schema(description = "AsyncPipeline is used for situations such as processing streams of data. The idea is that an AsyncPipeline" +
        " may perform execution in the background (internally); when the AsyncPipeline is queried, it may return the last processed" +
        " Data output. The behaviour of the AsyncPipeline is determined by the underlying Trigger.<br>" +
        "Note that because of the asynchronous nature of AsyncPipeline, the input Data instance may not actually be used " +
        "when executing the underlying pipeline. This means that AsyncPipeline is restricted to situations where the Data is loaded within " +
        "the pipeline itself, or no (external) input is required. For example, when an image (video frame) is loaded by FrameCaptureStep.")
public class AsyncPipeline implements Pipeline, AutoCloseable {

    @Schema(description = "The underlying pipeline")
    protected final Pipeline underlying;
    @Schema(description = "The async pipeline trigger")
    protected final Trigger trigger;
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    protected AsyncPipelineExecutor executor;

    public AsyncPipeline(@NonNull @JsonProperty("underlying") Pipeline underlying, @NonNull @JsonProperty("trigger") Trigger trigger){
        this.underlying = underlying;
        this.trigger = trigger;
    }

    @Override
    public synchronized PipelineExecutor executor() {
        if(executor == null){
            executor = new AsyncPipelineExecutor(this);
        }
        return executor;
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @Override
    public String id() {
        return underlying.id();
    }

    public void start(){
        //Thread is started in the underlying executor constructor
        executor();
    }

    @Override
    public void close() {
        trigger.stop();
    }
}
