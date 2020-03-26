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
package ai.konduit.serving.config.metrics;

import ai.konduit.serving.config.metrics.impl.MetricsBinderRendererAdapter;
import ai.konduit.serving.model.DL4JConfig;
import ai.konduit.serving.util.ObjectMappers;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * A no op {@link MetricsConfig}
 * for use with the {@link MetricsBinderRendererAdapter}
 *
 * @author Adam Gibson
 */
@Builder
@NoArgsConstructor
@Data
public class NoOpMetricsConfig implements MetricsConfig {
    @Override
    public Class<? extends MeterBinder> metricsBinderImplementation() {
        return MetricsBinderRendererAdapter.class;
    }

    @Override
    public Map<String, Object> configValues() {
        return Collections.emptyMap();
    }

    public static NoOpMetricsConfig fromJson(String json){
        return ObjectMappers.fromJson(json, NoOpMetricsConfig.class);
    }

    public static NoOpMetricsConfig fromYaml(String yaml){
        return ObjectMappers.fromYaml(yaml, NoOpMetricsConfig.class);
    }

}
