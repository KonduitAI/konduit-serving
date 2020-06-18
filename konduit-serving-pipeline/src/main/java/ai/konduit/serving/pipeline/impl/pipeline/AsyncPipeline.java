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
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class AsyncPipeline implements Pipeline, AutoCloseable {

    protected final Pipeline underlying;
    protected final Trigger trigger;
    protected AsyncPipelineExecutor executor;

    public AsyncPipeline(@NonNull Pipeline underlying, @NonNull Trigger trigger){
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
