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

import ai.konduit.serving.pipeline.api.pipeline.LoopTrigger;
import ai.konduit.serving.pipeline.api.pipeline.Pipeline;
import ai.konduit.serving.pipeline.api.pipeline.PipelineExecutor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class LoopPipeline implements Pipeline {

    protected final Pipeline underlying;
    protected final LoopTrigger loopTrigger;

    public LoopPipeline(Pipeline underlying, LoopTrigger loopTrigger){
        this.underlying = underlying;
        this.loopTrigger = loopTrigger;
    }

    @Override
    public PipelineExecutor executor() {
        return null;
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @Override
    public String id() {
        return underlying.id();
    }
}
