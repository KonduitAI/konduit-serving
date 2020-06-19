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

package ai.konduit.serving.pipeline.impl.pipeline.loop;

import ai.konduit.serving.annotation.json.JsonName;
import ai.konduit.serving.pipeline.api.data.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.concurrent.TimeUnit;

/**
 * A trigger to be used with an {@link ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline}.<br>
 * TimeLoopTrigger performs execution of the underlying executor every time unit (every minute, every 2 hours, etc)
 * at the start of the time unit (start of the minute, hour, etc), or at the start + an optional offset.
 * <br>
 * Optionally, a fixed input Data instance may be provided that is fed into the pipeline at each call of the underlying
 * pipeline (when executed in an async manner). If this is not provided, execution is performed using Data.empty() as input.
 * @author Alex Black
 */
@Schema(description = "A trigger to be used with an {@link ai.konduit.serving.pipeline.impl.pipeline.AsyncPipeline}.<br>" +
        "TimeLoopTrigger performs execution of the underlying executor every time unit (every minute, every 2 hours, etc)" +
        "at the start of the time unit (start of the minute, hour, etc), or at the start + an optional offset.<br>" +
        "Optionally, a fixed input Data instance may be provided that is fed into the pipeline at each call of the underlying " +
        "pipeline (when executed in an async manner). If this is not provided, execution is performed using Data.empty() as input.")
@lombok.Data
@EqualsAndHashCode(callSuper = true)
@JsonName("TIME_LOOP_TRIGGER")
public class TimeLoopTrigger extends SimpleLoopTrigger {

    protected final long offset;

    protected TimeLoopTrigger(@JsonProperty("frequencyMs") Long frequencyMs, @JsonProperty("offset") long offset,
                              @JsonProperty("data") Data data) {
        super(frequencyMs, data);
        this.offset = offset;
    }

    public TimeLoopTrigger(long duration, TimeUnit unit) {
        this(duration, unit, 0);
    }

    public TimeLoopTrigger(long duration, TimeUnit unit, long offset) {
        super(unit.toMillis(duration));
        this.offset = offset;
    }

    @Override
    protected long firstRunDelay() {
        long now = System.currentTimeMillis();
        long next = now + frequencyMs - (now % frequencyMs) + offset;
        return next - now;
    }

    @Override
    protected long nextStart(long lastStart) {
        long now = System.currentTimeMillis();
        return now + frequencyMs - (now % frequencyMs) + offset;
    }
}
