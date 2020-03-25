/*
 *
 *  * ******************************************************************************
 *  *
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classification metrics for counting number of classes
 * that occur during inference.
 *
 * @author Adam Gibson
 */
public class ClassificationMetrics implements MeterBinder {

    private List<String> labels;
    private Iterable<Tag> tags;
    @Getter
    private List<Counter> classCounters;

    public ClassificationMetrics(List<String> labels) {
        this(labels, Collections.emptyList());
    }

    public ClassificationMetrics(List<String> labels, Iterable<Tag> tags) {
        this.labels = labels;
        this.tags = tags;
        classCounters = new ArrayList<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for(int i = 0; i < labels.size(); i++) {
            classCounters.add(Counter.builder(labels.get(i))
                    .tags(tags)
                    .baseUnit("classification.outcome")
                    .register(meterRegistry));


        }
    }
}
