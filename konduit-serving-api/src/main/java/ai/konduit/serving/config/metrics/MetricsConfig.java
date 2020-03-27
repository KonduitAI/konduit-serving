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

import ai.konduit.serving.config.TextConfig;
import ai.konduit.serving.config.metrics.impl.ClassificationMetricsConfig;
import ai.konduit.serving.config.metrics.impl.RegressionMetricsConfig;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;

import java.util.Map;

/**
 * An {@link TextConfig} associated with
 * {@link MeterBinder} implementations provided as part of konduit-serving
 *
 * @author Adam Gibson
 */

@JsonSubTypes({
        @JsonSubTypes.Type(value = RegressionMetricsConfig.class, name = "RegressionMetricsConfig"),
        @JsonSubTypes.Type(value = NoOpMetricsConfig.class, name = "NoOpMetricsConfig"),
        @JsonSubTypes.Type(value = ClassificationMetricsConfig.class, name = "ClassificationMetricsConfig"),
})
public interface MetricsConfig extends TextConfig {

    /**
     * {@link MeterBinder} implementation associated with this configuration
     * @return teh meter binder class associated with this configuration
     */
    Class<? extends MeterBinder> metricsBinderImplementation();

    /**
     * The configuration value separated by name
     * and value
     * @return
     */
    Map<String,Object> configValues();
}
