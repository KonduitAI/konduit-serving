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
package ai.konduit.serving.pipeline.api.context;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PipelineMetrics implements Metrics {
    private String pipelineName;
    @Setter
    private String instanceName = "default";
    @Setter
    private String stepName = "default";

    private Map<String,PipelineGauge> gaugeMap = new HashMap<>();

    public PipelineMetrics(String name) {
        this.pipelineName = name;
    }

    private String assembleId(String id) {
        String effectiveId = pipelineName + "." + instanceName + "." + stepName + "." + id;
        return effectiveId;
    }

    @Override
    public Counter counter(String id) {
        Counter counter = new PipelineCounter(assembleId(id));
        return counter;
    }

    @Override
    public Timer timer(String id) {
        Timer timer = new PipelineTimer(assembleId(id));
        return timer;
    }

    @Override
    public Gauge gauge(String id, double number) {
        String s = assembleId(id);
        PipelineGauge pg = gaugeMap.computeIfAbsent(s, k -> new PipelineGauge(k, number));
        pg.set(number);
        return pg;
    }
}
