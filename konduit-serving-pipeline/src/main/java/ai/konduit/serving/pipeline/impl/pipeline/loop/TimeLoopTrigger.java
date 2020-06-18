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
import lombok.Data;
import org.nd4j.shade.jackson.annotation.JsonProperty;

import java.util.concurrent.TimeUnit;

@Data
@JsonName("TIME_LOOP_TRIGGER")
public class TimeLoopTrigger extends SimpleLoopTrigger {

    protected final long offset;

    protected TimeLoopTrigger(@JsonProperty("frequencyMs") Long frequencyMs, @JsonProperty("offset") long offset){
        super(frequencyMs);
        this.offset = offset;
    }

    public TimeLoopTrigger(long duration, TimeUnit unit) {
        this(duration, unit, 0);
    }

    public TimeLoopTrigger(long duration, TimeUnit unit, long offset){
        super(unit.toMillis(duration));
        this.offset = offset;
    }

    @Override
    protected long firstRunDelay(){
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
