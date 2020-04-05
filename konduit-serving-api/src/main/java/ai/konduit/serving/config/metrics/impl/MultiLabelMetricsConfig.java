/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
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
package ai.konduit.serving.config.metrics.impl;

import ai.konduit.serving.config.metrics.MetricsConfig;
import ai.konduit.serving.util.ObjectMappers;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A metrics configuration for a metrics render
 * that, given a set of specified labels
 * takes in counts of columns to increment.
 * The input is either a matrix or a vector representing the columns
 * to increment the count by. The column counts should be the same order
 * as the specified labels for this configuration.
 *
 * @author Adam Gibson
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MultiLabelMetricsConfig implements MetricsConfig {
    @Getter
    private List<String> labels;

    @SneakyThrows
    @Override
    public Class<? extends MeterBinder> metricsBinderImplementation() {
        return (Class<? extends MeterBinder>) Class.forName("ai.konduit.serving.metrics.MultiLabelMetrics");
    }

    @Override
    public Map<String, Object> configValues() {
        return Collections.singletonMap("labels",labels);
    }


    public static MultiLabelMetricsConfig fromJson(String json) {
        return ObjectMappers.fromJson(json, MultiLabelMetricsConfig.class);
    }

    public static MultiLabelMetricsConfig fromYaml(String yaml) {
        return ObjectMappers.fromYaml(yaml, MultiLabelMetricsConfig.class);
    }

}
